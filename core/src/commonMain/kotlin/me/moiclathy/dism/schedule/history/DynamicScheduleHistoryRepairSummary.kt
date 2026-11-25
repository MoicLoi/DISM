package me.moiclathy.dism.schedule.history

/**
 * 13.8.7
 *
 * 恢复计划汇总。
 *
 * 直接对应 UI 上那一块"历史完整性"：
 *
 * ```text
 * ✓ 126 条完整
 * ⚠ 3 条数据缺失
 * ✗ 1 条数据冲突
 *
 * 可自动处理：0
 * 需要人工确认：4
 * ```
 *
 * 数字全部来自真实的检查结果，
 * UI 层不应该自己重新扫一遍数据库。
 */
data class DynamicScheduleHistoryRepairSummary(
    val plans:
        List<DynamicScheduleHistoryRepairPlan>,
) {

    val planCount: Int
        get() = plans.size

    val plansRequiringAction: Int
        get() =
            plans.count {
                it.requiresAction
            }

    val plansRequiringManualReview: Int
        get() =
            plans.count {
                it.requiresManualReview
            }

    /**
     * 可安全自动执行的动作数量。
     *
     * 按 **snapshotId 去重**：孤儿是全局的，
     * 同一个孤儿会出现在每个 item 的计划里，
     * 直接求和会把"1 个孤儿"报成"N 个"。
     */
    val safeAutomaticActionCount: Int
        get() =
            plans
                .flatMap { plan ->
                    plan.actions
                }
                .filterIsInstance<
                    DynamicScheduleHistoryRepairAction
                    .DeleteOrphanReplayInput,
                    >()
                .map { it.snapshotId }
                .distinct()
                .size

    /**
     * 是否存在任何需要用户/上层处理的历史问题。
     */
    val needsAttention: Boolean
        get() =
            plans.any {
                it.requiresAction
            }
}
