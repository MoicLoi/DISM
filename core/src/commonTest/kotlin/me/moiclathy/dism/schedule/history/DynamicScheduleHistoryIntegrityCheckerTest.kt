package me.moiclathy.dism.schedule.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.7.10 / 13.7.11
 *
 * 完整性检查矩阵。
 *
 * 设计稿要求的 8 种情况 + 重复 itemId + 完整历史全部覆盖。
 */
class DynamicScheduleHistoryIntegrityCheckerTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val checker =
        DynamicScheduleHistoryIntegrityChecker(
            snapshotStore = snapshots,
            traceStore = traces,
            replayInputStore = replayInputs,
        )

    // -----------------------------------------------------------------
    // 完整历史
    // -----------------------------------------------------------------

    @Test
    fun completeDecisionHistoryIsValid() = runTest {

        saveCompleteDecision()

        val report = checker.check(itemId)

        assertEquals(
            DynamicScheduleHistoryIntegrityStatus.VALID,
            report.status,
        )

        assertTrue(report.issues.isEmpty())

        assertTrue(report.isValid)

        assertEquals(1, report.checkedSnapshots)
    }

    @Test
    fun noHistoryIsValidNotBroken() = runTest {

        /*
         * 首次启动 / 从未产生过决策，
         * 不是"坏数据"。
         */
        val report = checker.check(itemId)

        assertEquals(
            DynamicScheduleHistoryIntegrityStatus.VALID,
            report.status,
        )

        assertEquals(0, report.checkedSnapshots)
    }

    // -----------------------------------------------------------------
    // DEGRADED：缺一部分
    // -----------------------------------------------------------------

    @Test
    fun missingTraceIsDegraded() = runTest {

        val fixture = saveCompleteDecision()

        traces.delete(fixture.trace.traceId)

        val report = checker.check(itemId)

        assertEquals(
            DynamicScheduleHistoryIntegrityStatus.DEGRADED,
            report.status,
        )

        assertTrue(
            report.issues.single()
                is DynamicScheduleHistoryIntegrityIssue
                .MissingTrace,
        )
    }

    @Test
    fun snapshotWithoutTraceIsDegraded() = runTest {

        val fixture = saveCompleteDecision()

        /*
         * 模拟 12.15 之前的旧数据：没有 traceId。
         */
        val withoutTrace =
            fixture.snapshot.copy(
                id = DynamicScheduleDecisionSnapshotId(),
                traceId = null,
            )

        snapshots.save(withoutTrace)

        val report = checker.check(itemId)

        assertTrue(report.isDegraded)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .SnapshotWithoutTrace &&
                    it.snapshotId == withoutTrace.id
            },
        )
    }

    @Test
    fun missingReplayInputIsDegraded() = runTest {

        val fixture = saveCompleteDecision()

        replayInputs.delete(fixture.snapshot.id)

        val report = checker.check(itemId)

        assertEquals(
            DynamicScheduleHistoryIntegrityStatus.DEGRADED,
            report.status,
        )

        assertTrue(
            report.issues.single()
                is DynamicScheduleHistoryIntegrityIssue
                .MissingReplayInput,
        )
    }

    // -----------------------------------------------------------------
    // BROKEN：互相矛盾
    // -----------------------------------------------------------------

    @Test
    fun traceItemMismatchIsBroken() = runTest {

        val fixture = saveCompleteDecision()

        val otherItemId =
            DynamicScheduleItemId("item-2")

        traces.save(
            fixture.trace.copy(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(otherItemId)
                        .copy(id = fixture.trace.traceId),
            ),
        )

        val report = checker.check(itemId)

        assertEquals(
            DynamicScheduleHistoryIntegrityStatus.BROKEN,
            report.status,
        )

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .TraceItemMismatch
            },
        )
    }

    @Test
    fun scheduleRevisionMismatchIsBroken() = runTest {

        val fixture = saveCompleteDecision()

        traces.save(
            fixture.trace.copy(
                scheduleRevision =
                    fixture.trace.scheduleRevision + 1,
            ),
        )

        val report = checker.check(itemId)

        assertTrue(report.isBroken)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .ScheduleRevisionMismatch
            },
        )
    }

    @Test
    fun engineVersionMismatchIsBroken() = runTest {

        val fixture = saveCompleteDecision()

        traces.save(
            fixture.trace.copy(
                engineVersion = "99.9",
            ),
        )

        val report = checker.check(itemId)

        assertTrue(report.isBroken)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .EngineVersionMismatch
            },
        )
    }

    @Test
    fun replayInputRevisionMismatchIsBroken() = runTest {

        val fixture = saveCompleteDecision()

        replayInputs.save(
            fixture.replayInput.copy(
                scheduleRevision =
                    fixture.replayInput.scheduleRevision + 3,
            ),
        )

        val report = checker.check(itemId)

        assertTrue(report.isBroken)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputRevisionMismatch
            },
        )
    }

    @Test
    fun replayInputEngineVersionMismatchIsBroken() =
        runTest {

            val fixture = saveCompleteDecision()

            replayInputs.save(
                fixture.replayInput.copy(
                    engineVersion = "99.9",
                ),
            )

            val report = checker.check(itemId)

            assertTrue(report.isBroken)

            assertTrue(
                report.issues.any {
                    it is DynamicScheduleHistoryIntegrityIssue
                        .ReplayInputEngineVersionMismatch
                },
            )
        }

    @Test
    fun replayInputItemMismatchIsBroken() = runTest {

        val fixture = saveCompleteDecision()

        val otherItemId =
            DynamicScheduleItemId("item-2")

        replayInputs.save(
            fixture.replayInput.copy(
                decisionSnapshot =
                    fixture.snapshot.copy(
                        itemId = otherItemId,
                    ),
            ),
        )

        val report = checker.check(itemId)

        assertTrue(report.isBroken)

        assertTrue(
            report.issues.any {
                it is DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputItemMismatch
            },
        )
    }

    // -----------------------------------------------------------------
    // 批量
    // -----------------------------------------------------------------

    @Test
    fun duplicateItemIdsAreCheckedOnce() = runTest {

        saveCompleteDecision()

        val batch =
            checker.check(
                listOf(itemId, itemId, itemId),
            )

        assertEquals(1, batch.checkedItems)

        assertEquals(1, batch.validItems)

        assertTrue(batch.isHealthy)
    }

    @Test
    fun batchReportAggregatesStatuses() = runTest {

        saveCompleteDecision()

        val degradedItemId =
            DynamicScheduleItemId("item-2")

        val brokenItemId =
            DynamicScheduleItemId("item-3")

        /* item-2：只有 Snapshot，没有 Trace / ReplayInput */
        snapshots.save(
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(itemId = degradedItemId),
        )

        /* item-3：Snapshot 与 ReplayInput 的 revision 矛盾 */
        val brokenSnapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(itemId = brokenItemId)

        snapshots.save(brokenSnapshot)

        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(
                    decisionSnapshot = brokenSnapshot,
                    scheduleRevision =
                        brokenSnapshot.scheduleRevision + 5,
                ),
        )

        val batch =
            checker.check(
                listOf(itemId, degradedItemId, brokenItemId),
            )

        assertEquals(3, batch.checkedItems)

        assertEquals(1, batch.validItems)

        assertEquals(1, batch.degradedItems)

        assertEquals(1, batch.brokenItems)

        assertTrue(!batch.isHealthy)
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private class Fixture(
        val snapshot: DynamicScheduleDecisionSnapshot,

        val trace: DynamicScheduleDecisionTraceRecord,

        val replayInput:
            DynamicScheduleDecisionReplayInputSnapshot,
    )

    private suspend fun saveCompleteDecision(): Fixture {

        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        val replayInput =
            DynamicScheduleReplayTestData
                .create()
                .copy(decisionSnapshot = snapshot)

        val trace =
            DynamicScheduleDecisionTraceRecord(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(itemId)
                        .copy(id = snapshot.traceId!!),

                createdAt =
                    snapshot.capturedAt - 1.hours,

                scheduleRevision =
                    snapshot.scheduleRevision,

                engineVersion =
                    snapshot.engineVersion,
            )

        snapshots.save(snapshot)

        traces.save(trace)

        replayInputs.save(replayInput)

        return Fixture(
            snapshot = snapshot,
            trace = trace,
            replayInput = replayInput,
        )
    }
}
