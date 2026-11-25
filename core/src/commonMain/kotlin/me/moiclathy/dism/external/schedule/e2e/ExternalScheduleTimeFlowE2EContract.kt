package me.moiclathy.dism.external.schedule.e2e

object ExternalScheduleTimeFlowE2EContract {

    fun validate(
        trace: ExternalScheduleTimeFlowE2ETrace,
    ): List<String> {

        val errors =
            mutableListOf<String>()

        val requiredPhases =
            ExternalScheduleTimeFlowE2EPhase
                .entries

        for (phase in requiredPhases) {

            if (
                trace.steps.none {
                    it.phase == phase
                }
            ) {
                errors +=
                    "Missing E2E phase: $phase"
            }
        }

        trace.steps
            .filter {
                !it.successful
            }
            .forEach {
                errors +=
                    "Failed phase ${it.phase}: ${it.message}"
            }

        if (
            trace.normalizedEntryCount >
            trace.externalEntryCount
        ) {
            errors +=
                "Normalized entry count exceeds source entry count"
        }

        if (
            trace.eventDraftCount >
            trace.normalizedEntryCount
        ) {
            errors +=
                "Event draft count exceeds normalized entry count"
        }

        if (
            trace.persistedRecordCount >
            trace.eventDraftCount
        ) {
            errors +=
                "Persisted record count exceeds event draft count"
        }

        if (
            trace.recoveredRecordCount >
            trace.persistedRecordCount
        ) {
            errors +=
                "Recovered record count exceeds persisted record count"
        }

        if (
            trace.mappedEventCount >
            trace.recoveredRecordCount
        ) {
            errors +=
                "Mapped event count exceeds recovered record count"
        }

        if (
            trace.timeFlowProcessedCount >
            trace.mappedEventCount
        ) {
            errors +=
                "Time-flow processed count exceeds mapped event count"
        }

        return errors
    }

    fun requireValid(
        trace: ExternalScheduleTimeFlowE2ETrace,
    ) {

        val errors =
            validate(trace)

        require(errors.isEmpty()) {
            errors.joinToString(
                separator = "\n"
            )
        }
    }
}
