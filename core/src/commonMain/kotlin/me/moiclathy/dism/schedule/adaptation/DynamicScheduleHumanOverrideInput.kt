package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.10
 *
 * Human Override 的输入。
 *
 * 这里直接复用 12.x 已有的
 * [DynamicSchedulePriorityAdaptation]
 * （basePriority / adaptationFactor / effectivePriority），
 * 而不是另造一套对象。
 */
data class DynamicScheduleHumanOverrideInput(
    val adaptations:
        List<DynamicSchedulePriorityAdaptation>,

    val overrides:
        Map<DynamicScheduleItemId, DynamicScheduleHumanOverride> =
        emptyMap(),
)
