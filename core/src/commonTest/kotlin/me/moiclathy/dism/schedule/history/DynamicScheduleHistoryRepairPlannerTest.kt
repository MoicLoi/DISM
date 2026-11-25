package me.moiclathy.dism.schedule.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQuery
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.8.8
 *
 * 恢复计划的行为。
 *
 * 与设计稿的一点不同：这里不手工构造
 * [DynamicScheduleHistoryIntegrityReport]，
 * 而是让**真实的 13.7 检查器**跑出报告，再交给 planner。
 *
 * 因为 13.8 的价值完全取决于"13.7 报什么 → 13.8 建议什么"
 * 这条缝对不对；手写报告会把这条缝测没了。
 */
class DynamicScheduleHistoryRepairPlannerTest {

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

    private val planner =
        DynamicScheduleHistoryRepairPlanner()

    // -----------------------------------------------------------------
    // ① VALID → NoOp
    // -----------------------------------------------------------------

    @Test
    fun validHistoryProducesNoOpPlan() = runTest {

        saveCompleteDecision()

        val plan = plan()

        assertFalse(plan.requiresAction)

        assertFalse(plan.requiresManualReview)

        assertFalse(plan.hasSafeAutomaticAction)

        assertEquals(
            listOf(DynamicScheduleHistoryRepairAction.NoOp),
            plan.actions,
        )
    }

    @Test
    fun noHistoryAtAllAlsoProducesNoOpPlan() = runTest {

        val plan = plan()

        assertFalse(plan.requiresAction)

        assertEquals(
            listOf(DynamicScheduleHistoryRepairAction.NoOp),
            plan.actions,
        )
    }

    // -----------------------------------------------------------------
    // ② Missing Trace → Preserve
    // -----------------------------------------------------------------

    @Test
    fun missingTraceIsPreservedNotRebuilt() = runTest {

        val fixture = saveCompleteDecision()

        traces.delete(fixture.trace.traceId)

        val plan = plan()

        assertTrue(plan.requiresAction)

        assertTrue(plan.requiresManualReview)

        assertFalse(plan.hasSafeAutomaticAction)

        val action = plan.actions.single()

        assertTrue(
            action is DynamicScheduleHistoryRepairAction
                .PreserveMissingTrace,
        )

        assertEquals(
            fixture.trace.traceId,
            (
                action as
                    DynamicScheduleHistoryRepairAction
                    .PreserveMissingTrace
                ).traceId,
        )
    }

    // -----------------------------------------------------------------
    // ③ Missing ReplayInput → Preserve
    // -----------------------------------------------------------------

    @Test
    fun missingReplayInputIsPreserved() = runTest {

        val fixture = saveCompleteDecision()

        replayInputs.delete(fixture.snapshot.id)

        val plan = plan()

        assertTrue(plan.requiresAction)

        assertTrue(
            plan.actions.single()
                is DynamicScheduleHistoryRepairAction
                .PreserveMissingReplayInput,
        )
    }

    @Test
    fun snapshotWithoutTraceIsPreserved() = runTest {

        val fixture = saveCompleteDecision()

        snapshots.save(
            fixture.snapshot.copy(
                id =
                    DynamicScheduleDecisionSnapshotId(),

                traceId = null,
            ),
        )

        val plan = plan()

        assertTrue(
            plan.actions.any {
                it is DynamicScheduleHistoryRepairAction
                    .PreserveSnapshotWithoutTrace
            },
        )
    }

    // -----------------------------------------------------------------
    // ④ Revision mismatch → PreserveConflict（不是 RewriteXxx）
    // -----------------------------------------------------------------

    @Test
    fun revisionMismatchBecomesPreserveConflict() = runTest {

        val fixture = saveCompleteDecision()

        traces.save(
            fixture.trace.copy(
                scheduleRevision =
                    fixture.trace.scheduleRevision + 1,
            ),
        )

        val plan = plan()

        assertTrue(plan.requiresManualReview)

        val conflict =
            plan.actions.single()

        assertTrue(
            conflict is DynamicScheduleHistoryRepairAction
                .PreserveConflict,
        )

        /*
         * description 必须自带双方取值：
         * App 不需要再查一次数据库才能解释给用户。
         */
        val description =
            (
                conflict as
                    DynamicScheduleHistoryRepairAction
                    .PreserveConflict
                ).description

        assertTrue(
            description.contains(
                fixture.snapshot.scheduleRevision.toString(),
            ),
        )

        assertTrue(
            description.contains(
                (fixture.trace.scheduleRevision + 1)
                    .toString(),
            ),
        )
    }

    // -----------------------------------------------------------------
    // ⑤ Engine version mismatch → PreserveConflict
    // -----------------------------------------------------------------

    @Test
    fun engineVersionMismatchBecomesPreserveConflict() =
        runTest {

            val fixture = saveCompleteDecision()

            traces.save(
                fixture.trace.copy(engineVersion = "99.9"),
            )

            val plan = plan()

            val conflict =
                plan.actions.single() as
                    DynamicScheduleHistoryRepairAction
                    .PreserveConflict

            assertTrue(
                conflict.description.contains(
                    fixture.snapshot.engineVersion,
                ),
            )

            assertTrue(conflict.description.contains("99.9"))
        }

    @Test
    fun everyPlannedActionIsReadOnly() = runTest {

        val fixture = saveCompleteDecision()

        traces.save(
            fixture.trace.copy(
                scheduleRevision =
                    fixture.trace.scheduleRevision + 1,
            ),
        )

        replayInputs.delete(fixture.snapshot.id)

        val plan = plan()

        /*
         * 无论问题有多少，计划里都不允许出现
         * 任何"重写 / 重建"类动作。
         */
        assertTrue(
            plan.actions.all { action ->
                when (action) {
                    DynamicScheduleHistoryRepairAction.NoOp -> true

                    is DynamicScheduleHistoryRepairAction
                        .DeleteOrphanReplayInput,
                    is DynamicScheduleHistoryRepairAction
                        .PreserveMissingTrace,
                    is DynamicScheduleHistoryRepairAction
                        .PreserveMissingReplayInput,
                    is DynamicScheduleHistoryRepairAction
                        .PreserveSnapshotWithoutTrace,
                    is DynamicScheduleHistoryRepairAction
                        .PreserveConflict ->
                    true
                }
            },
        )

        assertTrue(plan.requiresManualReview)
    }

    // -----------------------------------------------------------------
    // ⑥ 规划过程不修改任何 Store
    // -----------------------------------------------------------------

    @Test
    fun planningDoesNotModifyAnyStore() = runTest {

        val fixture = saveCompleteDecision()

        /*
         * 制造一个 BROKEN 历史，
         * 也就是"最容易让人手痒去修"的那种情况。
         */
        traces.save(
            fixture.trace.copy(
                engineVersion = "99.9",
                scheduleRevision =
                    fixture.trace.scheduleRevision + 1,
            ),
        )

        val snapshotsBefore = allSnapshots()

        val tracesBefore = allTraces()

        val replayInputBefore =
            replayInputs.get(fixture.snapshot.id)

        val summary =
            DynamicScheduleHistoryRepairSummary(
                plans = listOf(plan()),
            )

        assertEquals(1, summary.plansRequiringManualReview)

        assertTrue(summary.needsAttention)

        assertEquals(0, summary.safeAutomaticActionCount)

        /*
         * 世界必须一模一样：一条都没删、一条都没改。
         */
        assertEquals(snapshotsBefore, allSnapshots())

        assertEquals(tracesBefore, allTraces())

        assertEquals(
            replayInputBefore,
            replayInputs.get(fixture.snapshot.id),
        )
    }

    @Test
    fun sessionLevelPlanningIsReadOnly() = runTest {

        val persistence = DismPersistence()

        val fixture = saveCompleteDecision(persistence)

        /*
         * 用会话层入口再走一遍，
         * 确认"App 能拿到的那个入口"也是只读的。
         */
        val session =
            me.moiclathy.dism.schedule.integration
                .DynamicScheduleIntegrationAssembly
                .from(persistence)
                .build()

        val plan = session.planHistoryRepair(itemId)

        assertFalse(plan.requiresAction)

        val summary =
            session.planHistoryRepair(
                listOf(itemId, itemId),
            )

        assertEquals(
            1,
            summary.planCount,
            "重复 itemId 只应该规划一次",
        )

        assertEquals(
            fixture.snapshot,
            persistence.scheduleSnapshots.get(
                fixture.snapshot.id,
            ),
        )

        assertEquals(
            fixture.trace,
            persistence.scheduleTraces.get(
                fixture.trace.traceId,
            ),
        )

        assertEquals(
            fixture.replayInput,
            persistence.scheduleReplayInputs.get(
                fixture.snapshot.id,
            ),
        )
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private suspend fun plan():
        DynamicScheduleHistoryRepairPlan =
        planner.plan(
            checker.check(itemId),
        )

    /**
     * 三个 Store 的完整内容，用来断言"什么也没被改动"。
     */
    private suspend fun allSnapshots():
        List<DynamicScheduleDecisionSnapshot> =
        snapshots
            .query(
                DynamicScheduleDecisionSnapshotQuery(
                    limit = Int.MAX_VALUE,
                ),
            )
            .snapshots

    private suspend fun allTraces():
        List<DynamicScheduleDecisionTraceRecord> =
        traces
            .query(
                DynamicScheduleDecisionTraceQuery(
                    limit = Int.MAX_VALUE,
                ),
            )
            .records

    private class Fixture(
        val snapshot: DynamicScheduleDecisionSnapshot,

        val trace: DynamicScheduleDecisionTraceRecord,

        val replayInput:
            DynamicScheduleDecisionReplayInputSnapshot,
    )

    private suspend fun saveCompleteDecision(
        persistence: DismPersistence? = null,
    ): Fixture {

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

                createdAt = snapshot.capturedAt,

                scheduleRevision =
                    snapshot.scheduleRevision,

                engineVersion =
                    snapshot.engineVersion,
            )

        if (persistence == null) {

            snapshots.save(snapshot)

            traces.save(trace)

            replayInputs.save(replayInput)

        } else {

            persistence.scheduleSnapshots.save(snapshot)

            persistence.scheduleTraces.save(trace)

            persistence.scheduleReplayInputs.save(replayInput)
        }

        return Fixture(
            snapshot = snapshot,
            trace = trace,
            replayInput = replayInput,
        )
    }
}
