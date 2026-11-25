package me.moiclathy.dism.schedule.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.14.15
 *
 * 决策时间线。
 *
 * 时间线是**纯读取视图**，因此这里不需要任何 Runtime。
 */
class DynamicScheduleDecisionTimelineTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val base: Instant =
        Instant.parse("2026-09-24T08:00:00Z")

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val service =
        DeterministicDynamicScheduleDecisionHistoryService(
            snapshotPersistence =
                DynamicScheduleDecisionSnapshotPersistenceService(
                    snapshots,
                ),

            tracePersistence =
                DynamicScheduleDecisionTracePersistenceService(
                    InMemoryDynamicScheduleDecisionTraceStore(),
                ),
        )

    private val timelineService =
        DynamicScheduleDecisionTimelineService(service)

    @Test
    fun entriesAreNewestFirst() = runTest {

        val a = saveSnapshot(0)

        val b = saveSnapshot(1)

        val c = saveSnapshot(2)

        val timeline = timelineService.timeline(itemId)

        assertEquals(
            listOf(c.id, b.id, a.id),
            timeline.entries.map { it.snapshotId },
        )

        assertEquals(c.id, timeline.latest?.snapshotId)

        assertEquals(a.id, timeline.earliest?.snapshotId)

        assertEquals(3, timeline.size)
    }

    @Test
    fun atReturnsTheMostRecentDecisionAtThatTime() = runTest {

        val a = saveSnapshot(0)

        val b = saveSnapshot(1)

        val c = saveSnapshot(2)

        /*
         * 09:30：
         *
         * A = 08:00 ✓
         * B = 09:00 ✓
         * C = 10:00 ✗
         *
         * "那个时刻当时最近一次已经发生的 Decision" 是 B。
         */
        assertEquals(
            b.id,
            timelineService
                .timeline(itemId)
                .at(base + 1.hours + 30.minutes)
                ?.snapshotId,
        )

        /* 08:30 时只有 A 发生过。 */
        assertEquals(
            a.id,
            timelineService
                .timeline(itemId)
                .at(base + 30.minutes)
                ?.snapshotId,
        )

        /* 10:00 之后是 C。 */
        assertEquals(
            c.id,
            timelineService
                .timeline(itemId)
                .at(base + 5.hours)
                ?.snapshotId,
        )
    }

    @Test
    fun atBeforeAnyDecisionIsNull() = runTest {

        saveSnapshot(1)

        assertNull(
            timelineService
                .timeline(itemId)
                .at(base - 1.hours),
        )
    }

    @Test
    fun beforeAndAfterKeepTimelineOrder() = runTest {

        val a = saveSnapshot(0)

        val b = saveSnapshot(1)

        val c = saveSnapshot(2)

        val timeline = timelineService.timeline(itemId)

        /*
         * before(10:00) → B, A（时间线顺序：最新在前）
         */
        assertEquals(
            listOf(b.id, a.id),
            timeline
                .before(base + 2.hours)
                .map { it.snapshotId },
        )

        /*
         * after(08:00) → C, B
         *
         * 注意设计稿期望 [B, C]（升序），
         * 但它自己的 before 期望是降序 [B, A]——
         * 两者不可能同时成立。
         *
         * 这里统一与 entries / latest / earliest 同一个方向。
         */
        assertEquals(
            listOf(c.id, b.id),
            timeline
                .after(base)
                .map { it.snapshotId },
        )
    }

    @Test
    fun betweenReturnsClosedRange() = runTest {

        saveSnapshot(0)

        val b = saveSnapshot(1)

        val c = saveSnapshot(2)

        val timeline = timelineService.timeline(itemId)

        assertEquals(
            listOf(c.id, b.id),
            timeline
                .between(
                    from = base + 1.hours,
                    until = base + 2.hours,
                )
                .map { it.snapshotId },
        )
    }

    @Test
    fun emptyTimelineIsSafe() = runTest {

        val timeline = timelineService.timeline(itemId)

        assertTrue(timeline.isEmpty)

        assertNull(timeline.latest)

        assertNull(timeline.earliest)

        assertNull(timeline.at(base))
    }

    @Test
    fun limitBoundsTheTimeline() = runTest {

        repeat(5) { index ->
            saveSnapshot(index)
        }

        assertEquals(
            2,
            timelineService.timeline(itemId, limit = 2).size,
        )
    }

    @Test
    fun otherItemsAreNotMixedIn() = runTest {

        saveSnapshot(0)

        saveSnapshot(
            0,
            itemId = DynamicScheduleItemId("other-item"),
        )

        val timeline = timelineService.timeline(itemId)

        assertEquals(1, timeline.size)

        assertEquals(itemId, timeline.itemId)
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private suspend fun saveSnapshot(
        hoursAgo: Int,

        itemId: DynamicScheduleItemId = this.itemId,
    ): DynamicScheduleDecisionSnapshot {

        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(
                    id = DynamicScheduleDecisionSnapshotId(),
                    itemId = itemId,
                    capturedAt = base + hoursAgo.hours,
                )

        snapshots.save(snapshot)

        return snapshot
    }

}
