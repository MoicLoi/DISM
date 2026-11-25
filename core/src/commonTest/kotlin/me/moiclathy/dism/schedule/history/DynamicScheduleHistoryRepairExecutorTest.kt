package me.moiclathy.dism.schedule.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

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
 * 13.9.16
 *
 * 受控修复执行边界。
 *
 * 这一组测试真正要保护的性质只有一条：
 *
 * > **Repair 不能为了"修好"而改写原始历史。**
 *
 * 因此第 2、3 个测试专门制造"看起来最该修"的情况
 * （缺 Trace、revision 矛盾），然后断言
 * Snapshot / Trace **一个字节都没变**。
 */
class DynamicScheduleHistoryRepairExecutorTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val analyzer =
        DynamicScheduleHistoryRepairAnalyzer(
            snapshotStore = snapshots,
            traceStore = traces,
            replayInputStore = replayInputs,
        )

    private val planner =
        DynamicScheduleHistoryRepairPlanner()

    private val executor =
        DefaultDynamicScheduleHistoryRepairExecutor(
            replayInputStore = replayInputs,
        )

    private val safePolicy =
        DynamicScheduleHistoryRepairPolicy(
            permission =
                DynamicScheduleHistoryRepairPermission.SAFE_ONLY,

            allowOrphanReplayInputDeletion = true,
        )

    // -----------------------------------------------------------------
    // Test 1：PLAN_ONLY 什么也不改
    // -----------------------------------------------------------------

    @Test
    fun planOnlyNeverChangesAnyData() = runTest {

        val fixture = saveCompleteDecision()

        val snapshotsBefore = allSnapshots()

        val tracesBefore = allTraces()

        val result =
            repair(
                policy =
                    DynamicScheduleHistoryRepairPolicy(),
            )

        assertFalse(result.changed)

        assertTrue(result.skipped)

        assertTrue(
            result.diagnostics.any {
                it.contains("PLAN_ONLY")
            },
        )

        assertEquals(snapshotsBefore, allSnapshots())

        assertEquals(tracesBefore, allTraces())

        assertNotNull(
            replayInputs.get(fixture.snapshot.id),
        )
    }

    // -----------------------------------------------------------------
    // Test 2：缺 Trace 不会被伪造
    // -----------------------------------------------------------------

    @Test
    fun missingTraceIsNotFabricated() = runTest {

        val fixture = saveCompleteDecision()

        traces.delete(fixture.trace.traceId)

        val result = repair(policy = safePolicy)

        assertFalse(
            result.changed,
            "SAFE_ONLY 也不允许重建历史 Trace",
        )

        assertTrue(
            result.skippedActions.any {
                it is DynamicScheduleHistoryRepairAction
                    .PreserveMissingTrace
            },
        )

        /*
         * 关键：Trace **不能**凭空出现。
         */
        assertNull(
            traces.get(fixture.trace.traceId),
            "原 Trace 不存在时，绝不能生成一条看起来一样的 Trace",
        )

        assertNotNull(
            snapshots.get(fixture.snapshot.id),
        )
    }

    // -----------------------------------------------------------------
    // Test 3：元数据矛盾不会被单边覆盖
    // -----------------------------------------------------------------

    @Test
    fun conflictingMetadataIsNeverOverwritten() = runTest {

        val fixture = saveCompleteDecision()

        val brokenTrace =
            fixture.trace.copy(
                scheduleRevision =
                    fixture.trace.scheduleRevision + 1,

                engineVersion = "99.9",
            )

        traces.save(brokenTrace)

        val result = repair(policy = safePolicy)

        assertFalse(
            result.changed,
            "矛盾历史不允许被自动改写",
        )

        /*
         * Snapshot 保持原值。
         */
        assertEquals(
            fixture.snapshot,
            snapshots.get(fixture.snapshot.id),
        )

        /*
         * Trace 也保持原值（就是那条"错的"）——
         * 系统并不知道哪一侧才是对的，
         * 所以两侧都不能动。
         */
        assertEquals(
            brokenTrace,
            traces.get(fixture.trace.traceId),
        )

        assertTrue(
            result.skippedActions.any {
                it is DynamicScheduleHistoryRepairAction
                    .PreserveConflict
            },
        )
    }

    // -----------------------------------------------------------------
    // Test 4：孤儿 ReplayInput 可以被安全清理
    // -----------------------------------------------------------------

    @Test
    fun orphanReplayInputIsDeletedUnderSafePolicy() = runTest {

        val fixture = saveCompleteDecision()

        val orphanSnapshotId =
            DynamicScheduleDecisionSnapshotId()

        /*
         * 造一个孤儿：有 ReplayInput，但没有对应 Snapshot。
         */
        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(
                    decisionSnapshot =
                        fixture.snapshot.copy(
                            id = orphanSnapshotId,
                        ),
                ),
        )

        val analysis = analyzer.analyze(itemId)

        assertEquals(
            listOf(orphanSnapshotId),
            analysis.orphanReplayInputSnapshotIds,
        )

        val result =
            executor.execute(
                itemId = itemId,
                plan = planner.plan(analysis),
                policy = safePolicy,
            )

        assertTrue(result.changed)

        assertEquals(
            1,
            result.executedActions.size,
        )

        assertNull(
            replayInputs.get(orphanSnapshotId),
            "孤儿 ReplayInput 应该被清理",
        )

        /*
         * 正常历史完全不受影响。
         */
        assertNotNull(
            snapshots.get(fixture.snapshot.id),
        )

        assertNotNull(
            traces.get(fixture.trace.traceId),
        )

        assertEquals(
            fixture.replayInput,
            replayInputs.get(fixture.snapshot.id),
        )
    }

    // -----------------------------------------------------------------
    // Test 5：PLAN_ONLY 下孤儿也不能删
    // -----------------------------------------------------------------

    @Test
    fun orphanIsNotDeletedUnderPlanOnly() = runTest {

        val fixture = saveCompleteDecision()

        val orphanSnapshotId =
            DynamicScheduleDecisionSnapshotId()

        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(
                    decisionSnapshot =
                        fixture.snapshot.copy(
                            id = orphanSnapshotId,
                        ),
                ),
        )

        val result =
            repair(
                policy =
                    DynamicScheduleHistoryRepairPolicy(),
            )

        assertFalse(result.changed)

        assertNotNull(
            replayInputs.get(orphanSnapshotId),
            "默认策略下不允许删除任何东西",
        )
    }

    @Test
    fun orphanDeletionAlsoRequiresTheExplicitFlag() = runTest {

        val fixture = saveCompleteDecision()

        val orphanSnapshotId =
            DynamicScheduleDecisionSnapshotId()

        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(
                    decisionSnapshot =
                        fixture.snapshot.copy(
                            id = orphanSnapshotId,
                        ),
                ),
        )

        /*
         * 只放开 permission、不放开 flag：
         * 仍然不允许删。
         */
        val result =
            repair(
                policy =
                    DynamicScheduleHistoryRepairPolicy(
                        permission =
                            DynamicScheduleHistoryRepairPermission
                                .SAFE_ONLY,
                    ),
            )

        assertFalse(result.changed)

        assertNotNull(
            replayInputs.get(orphanSnapshotId),
        )

        assertTrue(
            result.diagnostics.any {
                it.contains("disabled by policy")
            },
        )
    }

    @Test
    fun brokenHistoryMutationIsRejectedByPolicy() {

        /*
         * "允许修改 BROKEN 历史"与 PLAN_ONLY 是语义互斥的组合，
         * 应该在构造策略时就被拒绝，
         * 而不是留到运行期被悄悄忽略。
         */
        assertFailsWith<IllegalArgumentException> {            DynamicScheduleHistoryRepairPolicy(
                permission =
                    DynamicScheduleHistoryRepairPermission
                        .PLAN_ONLY,

                allowBrokenHistoryMutation = true,
            )
        }
    }

    // -----------------------------------------------------------------
    // Test 6：批量规划不会重复计算同一个孤儿
    // -----------------------------------------------------------------

    @Test
    fun duplicateItemIdsPlanOnceAndDoNotDoubleCountOrphans() =
        runTest {

            val fixture = saveCompleteDecision()

            val orphanSnapshotId =
                DynamicScheduleDecisionSnapshotId()

            replayInputs.save(
                DynamicScheduleReplayTestData
                    .create()
                    .copy(
                        decisionSnapshot =
                            fixture.snapshot.copy(
                                id = orphanSnapshotId,
                            ),
                    ),
            )

            val otherItemId =
                DynamicScheduleItemId("item-2")

            snapshots.save(
                DynamicScheduleReplayTestData
                    .decisionSnapshot()
                    .copy(itemId = otherItemId),
            )

            val summary =
                DynamicScheduleHistoryRepairSummary(
                    plans =
                        analyzer
                            .analyze(
                                listOf(
                                    itemId,
                                    itemId,
                                    otherItemId,
                                ),
                            )
                            .map(planner::plan),
                )

            assertEquals(
                2,
                summary.planCount,
                "重复 itemId 只规划一次",
            )

            /*
             * 孤儿是全局的，会出现在每个计划里，
             * 但汇总必须去重，否则 1 个孤儿会被报成 2 个。
             */
            assertEquals(
                1,
                summary.safeAutomaticActionCount,
            )
        }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private suspend fun repair(
        policy: DynamicScheduleHistoryRepairPolicy,
    ): DynamicScheduleHistoryRepairResult =
        executor.execute(
            itemId = itemId,

            plan =
                planner.plan(
                    analyzer.analyze(itemId),
                ),

            policy = policy,
        )

    private suspend fun allSnapshots():
        List<DynamicScheduleDecisionSnapshot> =
        snapshots
            .query(
                me.moiclathy.dism.schedule.snapshot
                    .DynamicScheduleDecisionSnapshotQuery(
                        limit = Int.MAX_VALUE,
                    ),
            )
            .snapshots

    private suspend fun allTraces():
        List<DynamicScheduleDecisionTraceRecord> =
        traces
            .query(
                me.moiclathy.dism.schedule.trace
                    .DynamicScheduleDecisionTraceQuery(
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

                createdAt = snapshot.capturedAt,

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
