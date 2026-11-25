package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 13.9
 *
 * 一次 Repair 执行的结果。
 *
 * 重点是 [diagnostics]：
 *
 * ```text
 * 不是"我修好了"，
 * 而是"我做了什么、以及为什么没做剩下的"。
 * ```
 *
 * 因为绝大多数情况下正确的结果就是"什么都没做"，
 * 上层必须能解释清楚为什么。
 */
data class DynamicScheduleHistoryRepairResult(
    val itemId: DynamicScheduleItemId,

    val plan: DynamicScheduleHistoryRepairPlan,

    val executedActions:
        List<DynamicScheduleHistoryRepairAction> = emptyList(),

    val skippedActions:
        List<DynamicScheduleHistoryRepairAction> = emptyList(),

    val diagnostics:
        List<String> = emptyList(),
) {

    val changed: Boolean
        get() = executedActions.isNotEmpty()

    val skipped: Boolean
        get() = skippedActions.isNotEmpty()

    /**
     * 是否需要上层/人工介入。
     *
     * 注意：只要有动作被跳过，就必须让人知道——
     * 静默跳过是最糟的结果，因为用户会以为历史已经修好了。
     */
    val requiresManualReview: Boolean
        get() =
            plan.requiresManualReview ||
                skippedActions.isNotEmpty()
}
