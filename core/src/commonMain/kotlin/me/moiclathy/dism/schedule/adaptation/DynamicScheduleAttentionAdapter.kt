package me.moiclathy.dism.schedule.adaptation

/**
 * 14.5
 *
 * 将 Dynamic Weight / Behavioral Model 转换成 Attention Adaptation。
 */
interface DynamicScheduleAttentionAdapter {

    fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        weightAdaptation:
            DynamicScheduleWeightAdaptation,

        policy:
            DynamicScheduleAttentionAdaptationPolicy =
            DynamicScheduleAttentionAdaptationPolicy(),
    ): DynamicScheduleAttentionAdaptation
}
