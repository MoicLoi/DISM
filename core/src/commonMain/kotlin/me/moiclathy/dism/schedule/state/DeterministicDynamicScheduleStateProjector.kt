package me.moiclathy.dism.schedule.state

import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.model.DynamicScheduleTemporal

/**
 * Deterministic temporal state projection for 12.2.
 *
 * This layer deliberately does not call the 10.x Event runtime. It only projects
 * the 12.1 schedule model from the current evaluation instant.
 */
class DeterministicDynamicScheduleStateProjector : DynamicScheduleStateProjector {
    override fun project(input: DynamicScheduleStateProjectionInput): DynamicScheduleStateProjectionResult {
        val changes = mutableListOf<DynamicScheduleStateProjectionChange>()
        val nextItems = input.model.items.map { item ->
            val decision = projectItem(item, input)
            if (decision.state != item.state) {
                changes += DynamicScheduleStateProjectionChange(
                    itemId = item.id,
                    previousState = item.state,
                    projectedState = decision.state,
                    reason = decision.reason,
                )
            }
            if (decision.state == item.state) item else item.copy(state = decision.state)
        }

        val nextModel = if (changes.isEmpty()) {
            input.model
        } else {
            input.model.copy(
                items = nextItems,
                generatedAt = input.evaluatedAt,
                revision = input.model.revision + 1,
            )
        }

        return DynamicScheduleStateProjectionResult(
            inputModel = input.model,
            projectedModel = nextModel,
            evaluatedAt = input.evaluatedAt,
            changes = changes,
        )
    }

    private fun projectItem(
        item: DynamicScheduleItem,
        input: DynamicScheduleStateProjectionInput,
    ): StateDecision {
        if (input.policy.preservesExistingState(item.state)) {
            return StateDecision(item.state, DynamicScheduleStateProjectionReason.PRESERVED_EXPLICIT_STATE)
        }

        return when (val temporal = item.temporal) {
            DynamicScheduleTemporal.Unscheduled ->
                StateDecision(DynamicScheduleState.UNSCHEDULED, DynamicScheduleStateProjectionReason.UNSCHEDULED)

            is DynamicScheduleTemporal.At ->
                if (input.evaluatedAt < temporal.startAt) {
                    StateDecision(DynamicScheduleState.UPCOMING, DynamicScheduleStateProjectionReason.BEFORE_START)
                } else {
                    StateDecision(input.policy.atStartedState, DynamicScheduleStateProjectionReason.START_REACHED)
                }

            is DynamicScheduleTemporal.Interval -> when {
                input.evaluatedAt < temporal.startAt ->
                    StateDecision(DynamicScheduleState.UPCOMING, DynamicScheduleStateProjectionReason.BEFORE_START)
                input.evaluatedAt < temporal.endAt ->
                    StateDecision(DynamicScheduleState.ACTIVE, DynamicScheduleStateProjectionReason.IN_PROGRESS)
                else ->
                    StateDecision(input.policy.intervalEndedState, DynamicScheduleStateProjectionReason.INTERVAL_ENDED)
            }

            is DynamicScheduleTemporal.Deadline ->
                if (input.evaluatedAt < temporal.deadline) {
                    StateDecision(DynamicScheduleState.READY, DynamicScheduleStateProjectionReason.BEFORE_DEADLINE)
                } else {
                    StateDecision(input.policy.deadlineReachedState, DynamicScheduleStateProjectionReason.DEADLINE_REACHED)
                }

            is DynamicScheduleTemporal.Window -> when {
                input.evaluatedAt < temporal.earliestStart ->
                    StateDecision(DynamicScheduleState.UPCOMING, DynamicScheduleStateProjectionReason.BEFORE_WINDOW)
                input.evaluatedAt <= temporal.latestStart ->
                    StateDecision(DynamicScheduleState.READY, DynamicScheduleStateProjectionReason.INSIDE_WINDOW)
                else ->
                    StateDecision(input.policy.windowExpiredState, DynamicScheduleStateProjectionReason.WINDOW_EXPIRED)
            }
        }
    }

    private data class StateDecision(
        val state: DynamicScheduleState,
        val reason: DynamicScheduleStateProjectionReason,
    )
}
