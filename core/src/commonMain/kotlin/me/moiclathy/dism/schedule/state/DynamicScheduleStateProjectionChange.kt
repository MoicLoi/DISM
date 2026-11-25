package me.moiclathy.dism.schedule.state

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

/** One explainable state transition produced by the temporal projection. */
data class DynamicScheduleStateProjectionChange(
    val itemId: DynamicScheduleItemId,
    val previousState: DynamicScheduleState,
    val projectedState: DynamicScheduleState,
    val reason: DynamicScheduleStateProjectionReason,
) {
    init {
        require(previousState != projectedState)
    }
}

enum class DynamicScheduleStateProjectionReason {
    UNSCHEDULED,
    BEFORE_START,
    START_REACHED,
    IN_PROGRESS,
    INTERVAL_ENDED,
    BEFORE_DEADLINE,
    DEADLINE_REACHED,
    BEFORE_WINDOW,
    INSIDE_WINDOW,
    WINDOW_EXPIRED,
    PRESERVED_EXPLICIT_STATE,
}
