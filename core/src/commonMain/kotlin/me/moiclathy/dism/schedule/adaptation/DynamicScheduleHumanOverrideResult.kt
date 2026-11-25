package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.10
 *
 * Human Override 的结果。
 *
 * `overriddenItemIds` 专门保留下来，让上层能区分：
 *
 * ```text
 * "这条是自动算出来的"
 * "这条被用户明确改过"
 * ```
 *
 * 两者在 UI 上必须不一样。
 */
data class DynamicScheduleHumanOverrideResult(
    val adaptations:
        List<DynamicSchedulePriorityAdaptation>,

    val overriddenItemIds:
        Set<DynamicScheduleItemId> = emptySet(),

    val diagnostics:
        List<String> = emptyList(),
) {
    val size: Int
        get() = adaptations.size
}
