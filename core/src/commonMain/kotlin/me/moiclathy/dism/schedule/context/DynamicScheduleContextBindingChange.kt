package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

data class DynamicScheduleContextBindingChange(
    val itemId: DynamicScheduleItemId,
    val previousState: DynamicScheduleState,
    val projectedState: DynamicScheduleState,
    val reason: DynamicScheduleContextBindingChangeReason,
    val match: DynamicScheduleContextMatch,
) {
    init {
        require(previousState != projectedState) {
            "a context binding change must actually change the state"
        }
    }
}

enum class DynamicScheduleContextBindingChangeReason {

    /**
     * A required context is currently missing.
     */
    REQUIRED_CONTEXT_MISSING,

    /**
     * An excluded context is currently active.
     */
    EXCLUDED_CONTEXT_ACTIVE,

    /**
     * Context requirements are satisfied and a previously context-blocked
     * item can become READY.
     */
    CONTEXT_SATISFIED,
}
