package me.moiclathy.dism.schedule.replay

class DeterministicDynamicScheduleReplayInputCompletenessChecker :
    DynamicScheduleReplayInputCompletenessChecker {

    override fun check(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,
    ): DynamicScheduleReplayInputCompleteness {

        val missing =
            buildList {

                if (
                    input
                        .input
                        .model
                        .items
                        .isEmpty()
                ) {
                    add(
                        "input.model.items",
                    )
                }

                if (
                    input
                        .input
                        .model
                        .revision < 0L
                ) {
                    add(
                        "input.model.revision",
                    )
                }

                if (
                    input
                        .policy
                        .attentionPolicy
                        .minimumPriority !in 0.0..1.0
                ) {
                    add(
                        "policy.attentionPolicy.minimumPriority",
                    )
                }

                if (
                    input
                        .evaluatedAt
                        .toString()
                        .isBlank()
                ) {
                    add(
                        "evaluatedAt",
                    )
                }

                if (
                    input.engineVersion.isBlank()
                ) {
                    add(
                        "engineVersion",
                    )
                }

                if (
                    input.scheduleRevision < 0L
                ) {
                    add(
                        "scheduleRevision",
                    )
                }
            }

        return if (missing.isEmpty()) {

            DynamicScheduleReplayInputCompleteness(
                complete = true,

                missingInputs = emptyList(),

                level =
                    DynamicScheduleReplayCompletenessLevel
                        .HISTORICAL_RUNTIME_REPLAY,
            )

        } else {

            DynamicScheduleReplayInputCompleteness(
                complete = false,

                missingInputs =
                    missing,

                level =
                    DynamicScheduleReplayCompletenessLevel
                        .SNAPSHOT_RECONSTRUCTION,
            )
        }
    }
}
