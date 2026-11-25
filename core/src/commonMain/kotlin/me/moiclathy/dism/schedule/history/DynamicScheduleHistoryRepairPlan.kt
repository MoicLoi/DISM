package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 13.8
 *
 * 某个 item 的历史恢复计划。
 *
 * 它回答的不是"我帮你修了"，而是：
 *
 * ```text
 * 当前历史状态：DEGRADED
 *
 * 建议：PreserveMissingTrace
 * 原因：原 Trace 不存在，无法无损重建
 * ```
 */
data class DynamicScheduleHistoryRepairPlan(
    val itemId: DynamicScheduleItemId,

    val integrityReport:
        DynamicScheduleHistoryIntegrityReport,

    val actions:
        List<DynamicScheduleHistoryRepairAction>,
) {

    val requiresAction: Boolean
        get() =
            actions.any {
                it !is DynamicScheduleHistoryRepairAction.NoOp
            }

    /**
     * 是否存在可以**自动**执行的安全动作。
     */
    val hasSafeAutomaticAction: Boolean
        get() =
            actions.any {
                it is
                    DynamicScheduleHistoryRepairAction
                    .DeleteOrphanReplayInput
            }

    /**
     * 是否需要人工/上层确认。
     *
     * 只要存在任何"保留"类动作，或者整体判定为 BROKEN，
     * 就必须走人工确认，不能被后台静默处理。
     */
    val requiresManualReview: Boolean
        get() =
            integrityReport.isBroken ||
                actions.any { action ->
                    when (action) {

                        is DynamicScheduleHistoryRepairAction
                            .PreserveMissingTrace,
                        is DynamicScheduleHistoryRepairAction
                            .PreserveMissingReplayInput,
                        is DynamicScheduleHistoryRepairAction
                            .PreserveSnapshotWithoutTrace,
                        is DynamicScheduleHistoryRepairAction
                            .PreserveConflict ->
                        true

                        DynamicScheduleHistoryRepairAction.NoOp ->
                            false

                        is DynamicScheduleHistoryRepairAction
                            .DeleteOrphanReplayInput ->
                            false
                    }
                }

    val preserveActionCount: Int
        get() =
            actions.count {
                it !is DynamicScheduleHistoryRepairAction.NoOp &&
                    it !is
                    DynamicScheduleHistoryRepairAction
                        .DeleteOrphanReplayInput
            }
}
