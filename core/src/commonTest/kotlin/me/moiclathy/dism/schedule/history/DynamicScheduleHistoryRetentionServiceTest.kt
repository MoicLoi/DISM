package me.moiclathy.dism.schedule.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.5.13
 *
 * 历史压缩的核心行为。
 *
 * fixture 直接复用 [DynamicScheduleReplayTestData] 的真实
 * Snapshot / ReplayInput，再按需覆盖 id / capturedAt / traceId，
 * 不另造一套"最小假 Snapshot"。
 */
@OptIn(ExperimentalUuidApi::class)
class DynamicScheduleHistoryRetentionServiceTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val now: Instant =
        Instant.parse("2026-09-24T08:00:00Z")

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val service =
        DynamicScheduleHistoryRetentionService(
            snapshotStore = snapshots,
            traceStore = traces,
            replayInputStore = replayInputs,
        )

    // -----------------------------------------------------------------
    // Test 1：最新永远保留
    // -----------------------------------------------------------------

    @Test
    fun latestSnapshotIsAlwaysPreserved() = runTest {

        val oldest = saveSnapshot(hoursAgo = 3)
        val middle = saveSnapshot(hoursAgo = 2)
        val latest = saveSnapshot(hoursAgo = 1)

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = 1,
                    ),
            )

        assertNotNull(
            snapshots.get(latest.id),
            "最新一次决策不能被压缩掉",
        )

        assertNull(snapshots.get(oldest.id))
        assertNull(snapshots.get(middle.id))

        assertEquals(2, result.snapshotsDeleted)
    }

    // -----------------------------------------------------------------
    // Test 2：按数量压缩
    // -----------------------------------------------------------------

    @Test
    fun oldSnapshotsAreCompactedByCount() = runTest {

        repeat(3) { index ->
            saveSnapshot(hoursAgo = 3 - index)
        }

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = 2,
                    ),
            )

        assertEquals(1, result.snapshotsDeleted)

        assertEquals(
            2,
            snapshots
                .query(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = itemId,
                    ),
                )
                .totalMatched,
        )
    }

    // -----------------------------------------------------------------
    // Test 3：按时间压缩
    // -----------------------------------------------------------------

    @Test
    fun oldSnapshotsAreCompactedByAge() = runTest {

        val expired =
            saveSnapshot(hoursAgo = 100)

        val fresh =
            saveSnapshot(hoursAgo = 1)

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = null,
                        maxAge = 2.days,
                    ),
            )

        assertNull(snapshots.get(expired.id))
        assertNotNull(snapshots.get(fresh.id))

        assertEquals(1, result.snapshotsDeleted)
    }

    // -----------------------------------------------------------------
    // Test 4：ReplayInput 与 Snapshot 同生共死
    // -----------------------------------------------------------------

    @Test
    fun replayInputIsDeletedWithSnapshot() = runTest {

        val kept = saveSnapshot(hoursAgo = 1)
        val removed = saveSnapshot(hoursAgo = 5)

        saveReplayInput(kept)
        saveReplayInput(removed)

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = 1,
                    ),
            )

        assertNull(snapshots.get(removed.id))
        assertNull(
            replayInputs.get(removed.id),
            "不能留下「能重放但没有决策」的孤儿输入",
        )

        assertNotNull(snapshots.get(kept.id))
        assertNotNull(replayInputs.get(kept.id))

        assertEquals(1, result.replayInputsDeleted)
    }

    // -----------------------------------------------------------------
    // Test 5：Trace 只在无人引用时删除
    // -----------------------------------------------------------------

    @Test
    fun traceIsDeletedOnlyWhenNoSnapshotReferencesIt() = runTest {

        val removed = saveSnapshot(hoursAgo = 5)
        val kept = saveSnapshot(hoursAgo = 1)

        saveTrace(removed)
        saveTrace(kept)

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = 1,
                    ),
            )

        assertNull(
            traces.get(removed.traceId!!),
            "被删 Snapshot 的 Trace 没有其它引用，必须一起删",
        )

        assertNotNull(
            traces.get(kept.traceId!!),
            "仍然被保留 Snapshot 引用的 Trace 不能被删",
        )

        assertEquals(1, result.tracesDeleted)
    }

    @Test
    fun sharedTraceSurvivesCompaction() = runTest {

        /*
         * 两个 Snapshot 共享同一条 Trace。
         *
         * 删除其中一个，Trace 仍然被另一个引用，
         * 所以必须保留。
         */
        val sharedTraceId =
            DynamicScheduleDecisionTraceId()

        val removed =
            saveSnapshot(
                hoursAgo = 5,
                traceId = sharedTraceId,
            )

        val kept =
            saveSnapshot(
                hoursAgo = 1,
                traceId = sharedTraceId,
            )

        saveTrace(kept)

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = 1,
                    ),
            )

        assertEquals(1, result.snapshotsDeleted)

        assertEquals(
            0,
            result.tracesDeleted,
            "共享 Trace 不能被压缩误删",
        )

        assertNotNull(traces.get(sharedTraceId))

        assertNull(snapshots.get(removed.id))
        assertNotNull(snapshots.get(kept.id))
    }

    // -----------------------------------------------------------------
    // Test 6 / 7：幂等与 no-op
    // -----------------------------------------------------------------

    @Test
    fun duplicateItemIdsAreCompactedOnce() = runTest {

        saveSnapshot(hoursAgo = 3)
        saveSnapshot(hoursAgo = 2)
        saveSnapshot(hoursAgo = 1)

        val result =
            service.compact(
                itemIds = listOf(itemId, itemId, itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = 1,
                    ),
            )

        assertEquals(
            1,
            result.results.size,
            "重复 itemId 不应该压缩多轮",
        )

        assertEquals(2, result.snapshotsDeleted)

        assertEquals(
            1,
            snapshots
                .query(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = itemId,
                    ),
                )
                .totalMatched,
        )
    }

    @Test
    fun compactionIsSafeNoOpWithoutHistory() = runTest {

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
            )

        assertEquals(0, result.totalDeleted)

        assertTrue(result.isEmpty)
    }

    @Test
    fun compactionIsNoOpWhenWithinLimits() = runTest {

        saveSnapshot(hoursAgo = 2)
        saveSnapshot(hoursAgo = 1)

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = 10,
                    ),
            )

        assertEquals(0, result.totalDeleted)
    }

    @Test
    fun preserveLatestFalseAllowsDeletingEverything() = runTest {

        val only = saveSnapshot(hoursAgo = 100)

        val result =
            service.compact(
                itemIds = listOf(itemId),
                now = now,
                policy =
                    DynamicScheduleHistoryRetentionPolicy(
                        maxSnapshotsPerItem = null,
                        maxAge = 1.days,
                        preserveLatest = false,
                    ),
            )

        assertEquals(1, result.snapshotsDeleted)

        assertNull(snapshots.get(only.id))
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private suspend fun saveSnapshot(
        hoursAgo: Int,

        traceId: DynamicScheduleDecisionTraceId =
            DynamicScheduleDecisionTraceId(),
    ): DynamicScheduleDecisionSnapshot {

        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(
                    id =
                        DynamicScheduleDecisionSnapshotId(),

                    capturedAt = now - hoursAgo.hours,

                    traceId = traceId,
                )

        snapshots.save(snapshot)

        return snapshot
    }

    private suspend fun saveTrace(
        snapshot: DynamicScheduleDecisionSnapshot,
    ) {
        traces.save(
            DynamicScheduleDecisionTraceRecord(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(itemId)
                        .copy(id = snapshot.traceId!!),

                createdAt = snapshot.capturedAt,

                scheduleRevision =
                    snapshot.scheduleRevision,
            ),
        )
    }

    private suspend fun saveReplayInput(
        snapshot: DynamicScheduleDecisionSnapshot,
    ) {
        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(decisionSnapshot = snapshot),
        )
    }
}
