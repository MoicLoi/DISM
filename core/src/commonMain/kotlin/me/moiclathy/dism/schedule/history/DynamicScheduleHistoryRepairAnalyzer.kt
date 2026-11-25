package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore

/**
 * 13.9.12
 *
 * 把"一个 item 的历史现状"整理成 Repair Planner 能消费的输入。
 *
 * 职责划分：
 *
 * ```text
 * IntegrityChecker  → 正向引用是否成立
 * OrphanDetector    → 反向引用是否已成孤儿
 * Analyzer          → 把两者拼成一次分析
 * Planner           → 分析 → 计划
 * Executor          → 计划 + 策略 → 结果
 * ```
 */
class DynamicScheduleHistoryRepairAnalyzer(
    private val snapshotStore:
        DynamicScheduleDecisionSnapshotStore,

    private val traceStore:
        DynamicScheduleDecisionTraceStore,

    private val replayInputStore:
        DynamicScheduleDecisionReplayInputSnapshotStore,
) {

    private val integrityChecker =
        DynamicScheduleHistoryIntegrityChecker(
            snapshotStore = snapshotStore,
            traceStore = traceStore,
            replayInputStore = replayInputStore,
        )

    private val orphanDetector =
        DynamicScheduleHistoryOrphanDetector(
            snapshotStore = snapshotStore,
            replayInputStore = replayInputStore,
        )

    suspend fun analyze(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleHistoryRepairAnalysis =
        DynamicScheduleHistoryRepairAnalysis(
            integrity = integrityChecker.check(itemId),

            /*
             * 孤儿检测是全库扫描，与 itemId 无关，
             * 因此这里的结果对每个 item 都是同一份。
             */
            orphanReplayInputSnapshotIds =
                orphanDetector.findOrphanReplayInputs(),
        )

    suspend fun analyze(
        itemIds: Collection<DynamicScheduleItemId>,
    ): List<DynamicScheduleHistoryRepairAnalysis> {

        /*
         * 先算一次孤儿列表，避免每个 item 都全库扫一遍。
         */
        val orphans =
            orphanDetector.findOrphanReplayInputs()

        return itemIds
            .distinct()
            .map { itemId ->
                DynamicScheduleHistoryRepairAnalysis(
                    integrity =
                        integrityChecker.check(itemId),

                    orphanReplayInputSnapshotIds = orphans,
                )
            }
    }
}
