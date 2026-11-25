package me.moiclathy.dism.schedule.adaptation

/**
 * 14.4
 *
 * ```text
 * Behavioral Model → Dynamic Weight
 * ```
 *
 * 它只产生"权重应该怎样偏移"，**不修改** Schedule / Attention / 数据库。
 */
interface DynamicScheduleWeightAdapter {

    fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        policy:
            DynamicScheduleWeightAdaptationPolicy =
            DynamicScheduleWeightAdaptationPolicy(),
    ): DynamicScheduleWeightAdaptation
}
