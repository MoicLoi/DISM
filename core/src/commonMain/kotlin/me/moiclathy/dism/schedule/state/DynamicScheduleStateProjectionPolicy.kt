package me.moiclathy.dism.schedule.state

import me.moiclathy.dism.schedule.model.DynamicScheduleState

/**
 * Controls which existing states the temporal projector is allowed to replace.
 *
 * The projector is intentionally conservative: explicit terminal or externally
 * controlled states should not be silently rewritten by time alone.
 */
data class DynamicScheduleStateProjectionPolicy(
    val preserveTerminalStates: Boolean = true,
    val preserveBlockedState: Boolean = true,
    val preservePostponedState: Boolean = true,
    val atStartedState: DynamicScheduleState = DynamicScheduleState.ACTIVE,
    val intervalEndedState: DynamicScheduleState = DynamicScheduleState.EXPIRED,
    val deadlineReachedState: DynamicScheduleState = DynamicScheduleState.EXPIRED,
    val windowExpiredState: DynamicScheduleState = DynamicScheduleState.EXPIRED,
) {
    init {
        require(atStartedState != DynamicScheduleState.UNSCHEDULED)
        require(intervalEndedState != DynamicScheduleState.UNSCHEDULED)
        require(deadlineReachedState != DynamicScheduleState.UNSCHEDULED)
        require(windowExpiredState != DynamicScheduleState.UNSCHEDULED)
    }

    fun preservesExistingState(state: DynamicScheduleState): Boolean =
        (preserveTerminalStates && state in terminalStates) ||
            (preserveBlockedState && state == DynamicScheduleState.BLOCKED) ||
            (preservePostponedState && state == DynamicScheduleState.POSTPONED)

    companion object {
        private val terminalStates = setOf(
            DynamicScheduleState.COMPLETED,
            DynamicScheduleState.CANCELLED,
            DynamicScheduleState.EXPIRED,
        )
    }
}
