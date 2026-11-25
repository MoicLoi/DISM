package me.moiclathy.dism.attention

import kotlinx.serialization.Serializable

@Serializable
data class AttentionPolicy(
    val softLimit: Int = 5,
    val hardLimit: Int = 8,
    val minimumPriority: Double = 0.25,
    /**
     * Priority at which a recommendation may consume the hard-limit overflow
     * budget when the normal soft attention budget is already full.
     */
    val criticalPriority: Double = 0.90,
    /**
     * Allows critical recommendations to exceed softLimit, but never hardLimit.
     * Disabled preserves the Stage 7.4 selection behavior.
     */
    val allowCriticalOverflow: Boolean = false,
) {
    init {
        require(softLimit >= 0)
        require(hardLimit >= softLimit)
        require(minimumPriority in 0.0..1.0)
        require(criticalPriority in 0.0..1.0)
    }
}
