package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.schedule.model.DynamicScheduleState
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleAttentionPolicy(
    /**
     * Items below this priority are not projected into Attention.
     */
    val minimumPriority: Double = 0.25,

    /**
     * Items below this confidence are not projected into Attention.
     */
    val minimumConfidence: Double = 0.25,

    /**
     * Whether BLOCKED schedule items should become attention candidates.
     *
     * Usually true because a blocked important event can be more
     * important than an ordinary READY event.
     */
    val includeBlocked: Boolean = true,

    /**
     * Whether POSTPONED items should remain visible to Attention.
     */
    val includePostponed: Boolean = true,

    /**
     * Terminal states should never become new attention candidates.
     */
    val excludedStates: Set<DynamicScheduleState> = setOf(
        DynamicScheduleState.COMPLETED,
        DynamicScheduleState.CANCELLED,
        DynamicScheduleState.EXPIRED,
        DynamicScheduleState.UNSCHEDULED,
    ),
) {
    init {
        require(minimumPriority in 0.0..1.0) {
            "minimumPriority must be in [0, 1]"
        }

        require(minimumConfidence in 0.0..1.0) {
            "minimumConfidence must be in [0, 1]"
        }
    }

    fun acceptsState(
        state: DynamicScheduleState,
    ): Boolean {
        if (state in excludedStates) {
            return false
        }

        if (
            state == DynamicScheduleState.BLOCKED &&
            !includeBlocked
        ) {
            return false
        }

        if (
            state == DynamicScheduleState.POSTPONED &&
            !includePostponed
        ) {
            return false
        }

        return true
    }
}
