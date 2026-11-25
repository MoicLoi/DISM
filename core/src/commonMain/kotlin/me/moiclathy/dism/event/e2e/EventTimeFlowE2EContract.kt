package me.moiclathy.dism.event.e2e

/**
 * Time-flow E2E Contract。
 *
 * 不负责执行任何业务逻辑，
 * 只验证各阶段之间的最终契约。
 */
object EventTimeFlowE2EContract {

    private val requiredPhases =
        EventTimeFlowE2EPhase.entries.toSet()

    fun validate(
        trace: EventTimeFlowE2ETrace,
    ): List<String> {

        val errors =
            mutableListOf<String>()

        val actualPhases =
            trace.steps
                .map { it.phase }
                .toSet()

        val missingPhases =
            requiredPhases - actualPhases

        if (missingPhases.isNotEmpty()) {

            errors +=
                "Missing E2E phases: " +
                    missingPhases
                        .sortedBy { it.ordinal }
                        .joinToString()
        }

        val failedSteps =
            trace.steps
                .filterNot { it.successful }

        failedSteps.forEach { step ->

            errors +=
                "Phase ${step.phase} failed: " +
                    step.message
        }

        if (trace.eventId.isBlank()) {

            errors +=
                "Event ID must not be blank."
        }

        if (trace.persistedEventCount <= 0) {

            errors +=
                "At least one runtime event must be persisted."
        }

        val recovery =
            trace.recoveryResult

        if (recovery == null) {

            errors +=
                "Recovery result is missing."

        } else {

            if (!recovery.recovered) {

                errors +=
                    "Runtime recovery did not recover a persisted state."
            }

            if (
                recovery.restoredFromEventId == null
            ) {

                errors +=
                    "Recovery source event ID is missing."
            }

            if (
                trace.runtimeState != null &&
                recovery.state != trace.runtimeState
            ) {

                errors +=
                    "Recovered runtime state does not match persisted runtime state."
            }
        }

        return errors
    }
}
