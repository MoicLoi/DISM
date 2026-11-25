package me.moiclathy.dism.schedule.context

import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleContextAttentionHistory(
    val key: DynamicScheduleContextAttentionKey,

    val shownCount: Int = 0,

    val engagedCount: Int = 0,

    val ignoredCount: Int = 0,

    val dismissedCount: Int = 0,

    val requestedCount: Int = 0,

    val feedbackSignal: Double = 0.0,

    val lastInteractionAt: Instant? = null,
) {
    init {
        require(shownCount >= 0)
        require(engagedCount >= 0)
        require(ignoredCount >= 0)
        require(dismissedCount >= 0)
        require(requestedCount >= 0)
        require(feedbackSignal in -1.0..1.0)
    }
}
