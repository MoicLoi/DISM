package me.moiclathy.dism.schedule.adaptation

/**
 * 14.6
 *
 * ```text
 * Behavioral Model / Dynamic Weight
 *     → Schedule Priority Influence
 * ```
 *
 * 注意它**不是** 12.x 的 `DynamicSchedulePriorityAdapter`：
 * 那个消费 Attention 历史并产出倍率，
 * 这个消费 14.x 的 Behavior Model + Weight Delta 并产出偏移量。
 */
interface DynamicScheduleBehavioralPriorityAdaptationAdapter {

    fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        weightAdaptation:
            DynamicScheduleWeightAdaptation,

        policy:
            DynamicScheduleBehavioralPriorityAdaptationPolicy =
            DynamicScheduleBehavioralPriorityAdaptationPolicy(),
    ): DynamicScheduleBehavioralPriorityAdaptation
}
