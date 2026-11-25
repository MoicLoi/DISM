package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.context.DynamicScheduleContextPriorityInfluence

/**
 * 14.8 的统一输入。
 */
data class DynamicScheduleAdaptationConfidenceInput(
    val priorityAdaptation:
        DynamicSchedulePriorityAdaptation,

    val historicalEvidence:
        Double = 0.0,

    val contextInfluence:
        DynamicScheduleContextPriorityInfluence? = null,

    /**
     * 行为稳定性。
     *
     * 0 = 完全不稳定
     * 1 = 高度稳定
     */
    val stabilityEvidence:
        Double = 0.0,

    /**
     * 有效历史样本量。
     */
    val sampleCount:
        Int = 0,
) {
    init {
        require(historicalEvidence in 0.0..1.0)
        require(stabilityEvidence in 0.0..1.0)
        require(sampleCount >= 0)
    }
}
