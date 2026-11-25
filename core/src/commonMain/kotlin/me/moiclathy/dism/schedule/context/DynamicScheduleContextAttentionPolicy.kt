package me.moiclathy.dism.schedule.context

data class DynamicScheduleContextAttentionPolicy(
    val minimumFactor: Double = 0.5,

    val maximumFactor: Double = 1.5,

    val ignoredPenalty: Double = 0.05,

    val dismissedPenalty: Double = 0.10,

    val engagedBonus: Double = 0.05,

    val requestedBonus: Double = 0.15,

    val feedbackWeight: Double = 0.25,

    /**
     * Context memory should not completely dominate
     * the normal schedule priority.
     */
    val contextInfluence: Double = 0.5,
) {
    init {
        require(minimumFactor in 0.0..1.0)
        require(maximumFactor >= 1.0)
        require(ignoredPenalty >= 0.0)
        require(dismissedPenalty >= 0.0)
        require(engagedBonus >= 0.0)
        require(requestedBonus >= 0.0)
        require(feedbackWeight >= 0.0)
        require(contextInfluence in 0.0..1.0)
    }
}
