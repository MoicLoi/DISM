package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlinx.serialization.Serializable

/**
 * Controls temporal decay of attention history.
 *
 * A value loses half of its influence after [halfLife].
 */
@Serializable
data class DynamicScheduleAttentionDecayPolicy(
    val halfLife: Duration = 30.days,

    /**
     * Old history is never completely discarded.
     *
     * This prevents the system from forgetting long-term behavior
     * too aggressively.
     */
    val minimumWeight: Double = 0.05,
) {
    init {
        require(!halfLife.isNegative())
        require(halfLife > Duration.ZERO)
        require(minimumWeight in 0.0..1.0)
    }
}
