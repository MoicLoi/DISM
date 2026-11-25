package me.moiclathy.dism.schedule.attention

import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleAttentionExplanationPolicy(
    val maximumReasons: Int = 4,

    val includeBackgroundReasons: Boolean = true,

    val minimumReasonStrength: Double = 0.1,
) {
    init {
        require(maximumReasons > 0)
        require(minimumReasonStrength in 0.0..1.0)
    }
}
