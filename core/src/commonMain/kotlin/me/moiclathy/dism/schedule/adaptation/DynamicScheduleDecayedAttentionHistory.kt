package me.moiclathy.dism.schedule.adaptation

data class DynamicScheduleDecayedAttentionHistory(
    val attentionKey: String,

    val shownWeight: Double = 0.0,

    val engagedWeight: Double = 0.0,

    val ignoredWeight: Double = 0.0,

    val dismissedWeight: Double = 0.0,

    val requestedWeight: Double = 0.0,

    /**
     * Explicit feedback after temporal decay.
     */
    val feedbackSignal: Double = 0.0,

    /**
     * Overall amount of usable historical evidence.
     */
    val evidenceWeight: Double = 0.0,
) {
    init {
        require(attentionKey.isNotBlank())

        require(shownWeight >= 0.0)
        require(engagedWeight >= 0.0)
        require(ignoredWeight >= 0.0)
        require(dismissedWeight >= 0.0)
        require(requestedWeight >= 0.0)

        require(feedbackSignal in -1.0..1.0)
        require(evidenceWeight >= 0.0)
    }
}
