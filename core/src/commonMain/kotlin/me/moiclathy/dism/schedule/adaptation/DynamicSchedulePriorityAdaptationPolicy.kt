package me.moiclathy.dism.schedule.adaptation

import kotlinx.serialization.Serializable

@Serializable
data class DynamicSchedulePriorityAdaptationPolicy(
    /**
     * 最低调整倍率。
     */
    val minimumFactor: Double = 0.5,

    /**
     * 最高调整倍率。
     */
    val maximumFactor: Double = 1.5,

    /**
     * ignored 对 priority 的影响。
     */
    val ignoredPenalty: Double = 0.05,

    /**
     * dismissed 对 priority 的影响。
     */
    val dismissedPenalty: Double = 0.10,

    /**
     * engaged 对 priority 的奖励。
     */
    val engagedBonus: Double = 0.05,

    /**
     * requested 对 priority 的奖励。
     */
    val requestedBonus: Double = 0.15,

    /**
     * 显式反馈的最大影响。
     */
    val feedbackWeight: Double = 0.25,
) {
    init {
        require(minimumFactor in 0.0..1.0)
        require(maximumFactor >= 1.0)
        require(ignoredPenalty >= 0.0)
        require(dismissedPenalty >= 0.0)
        require(engagedBonus >= 0.0)
        require(requestedBonus >= 0.0)
        require(feedbackWeight >= 0.0)
    }
}
