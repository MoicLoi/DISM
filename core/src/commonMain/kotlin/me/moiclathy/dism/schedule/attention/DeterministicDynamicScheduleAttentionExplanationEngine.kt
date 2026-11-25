package me.moiclathy.dism.schedule.attention

class DeterministicDynamicScheduleAttentionExplanationEngine :
    DynamicScheduleAttentionExplanationEngine {

    override fun explain(
        input: DynamicScheduleAttentionExplanationInput,
        policy: DynamicScheduleAttentionExplanationPolicy,
    ): DynamicScheduleAttentionExplanation {

        val reasons =
            buildReasons(
                input = input,
                policy = policy,
            )

        val summary =
            buildSummary(
                input = input,
                reasons = reasons,
            )

        return DynamicScheduleAttentionExplanation(
            itemId = input.item.id,
            summary = summary,
            reasons = reasons,
            resolvedPriority =
                input.conflictResult.resolvedPriority,
            attentionRequired =
                input.conflictResult.attentionRequired,
        )
    }

    private fun buildReasons(
        input: DynamicScheduleAttentionExplanationInput,
        policy: DynamicScheduleAttentionExplanationPolicy,
    ): List<DynamicScheduleAttentionExplanationReason> {

        val result =
            mutableListOf<
                DynamicScheduleAttentionExplanationReason
            >()

        val conflict =
            input.conflictResult.conflict

        /*
         * Hard constraints first.
         */
        conflict?.overriddenBy?.let { constraint ->

            val type =
                when (constraint.type) {

                    DynamicScheduleAttentionConstraintType
                        .REQUIRED_DEPENDENCY ->
                        DynamicScheduleAttentionExplanationReasonType
                            .REQUIRED_DEPENDENCY

                    DynamicScheduleAttentionConstraintType
                        .REQUIRED_CONTEXT ->
                        DynamicScheduleAttentionExplanationReasonType
                            .REQUIRED_CONTEXT

                    DynamicScheduleAttentionConstraintType
                        .HARD_TEMPORAL_ANCHOR ->
                        DynamicScheduleAttentionExplanationReasonType
                            .HARD_TEMPORAL_ANCHOR

                    DynamicScheduleAttentionConstraintType
                        .EXPLICIT_USER_REQUEST ->
                        DynamicScheduleAttentionExplanationReasonType
                            .EXPLICIT_USER_REQUEST

                    else ->
                        DynamicScheduleAttentionExplanationReasonType
                            .SCHEDULE_PRIORITY
                }

            result +=
                DynamicScheduleAttentionExplanationReason(
                    type = type,
                    message = constraint.reason,
                    strength = constraint.strength,
                    decisive = true,
                )
        }

        /*
         * Conflict itself is a useful explanation.
         */
        if (
            conflict?.type ==
                DynamicScheduleAttentionConflictType
                    .LEARNED_SUPPRESSION_VS_HARD_REQUIREMENT
        ) {

            result +=
                DynamicScheduleAttentionExplanationReason(
                    type =
                        DynamicScheduleAttentionExplanationReasonType
                            .CONSTRAINT_OVERRIDE,
                    message =
                        "This normally receives lower attention, " +
                            "but a required condition is currently active.",
                    strength = 1.0,
                    decisive = true,
                )
        }

        if (
            conflict?.type ==
                DynamicScheduleAttentionConflictType
                    .CONTEXT_MEMORY_CONFLICT
        ) {

            result +=
                DynamicScheduleAttentionExplanationReason(
                    type =
                        DynamicScheduleAttentionExplanationReasonType
                            .CONTEXT_ATTENTION_MEMORY,
                    message =
                        "Current context makes this more relevant " +
                            "than your usual behavior suggests.",
                    strength = 1.0,
                    decisive = true,
                )
        }

        /*
         * Context explanation.
         */
        input.contextReasons.forEach { reason ->

            result +=
                DynamicScheduleAttentionExplanationReason(
                    type =
                        DynamicScheduleAttentionExplanationReasonType
                            .REQUIRED_CONTEXT,
                    message = reason,
                    strength = 0.7,
                )
        }

        /*
         * Adaptation explanation.
         */
        input.adaptationReasons.forEach { reason ->

            result +=
                DynamicScheduleAttentionExplanationReason(
                    type =
                        DynamicScheduleAttentionExplanationReasonType
                            .GLOBAL_ATTENTION_MEMORY,
                    message = reason,
                    strength = 0.5,
                )
        }

        /*
         * Always preserve the base schedule fact.
         */
        result +=
            DynamicScheduleAttentionExplanationReason(
                type =
                    DynamicScheduleAttentionExplanationReasonType
                        .SCHEDULE_PRIORITY,
                message =
                    "Schedule priority=${input.item.priority}",
                strength =
                    input.item.priority,
            )

        return result
            .filter {
                it.strength >=
                    policy.minimumReasonStrength
            }
            .distinctBy {
                it.message
            }
            .sortedWith(
                compareByDescending<
                    DynamicScheduleAttentionExplanationReason
                > {
                    it.decisive
                }.thenByDescending {
                    it.strength
                },
            )
            .let {
                if (policy.includeBackgroundReasons) {
                    it.take(policy.maximumReasons)
                } else {
                    it.filter { reason ->
                        reason.decisive
                    }.take(policy.maximumReasons)
                }
            }
    }

    private fun buildSummary(
        input: DynamicScheduleAttentionExplanationInput,
        reasons:
            List<DynamicScheduleAttentionExplanationReason>,
    ): String {

        val conflict =
            input.conflictResult.conflict

        return when {

            conflict?.type ==
                DynamicScheduleAttentionConflictType
                    .EXPLICIT_REQUEST_OVERRIDE -> {

                "You explicitly asked to pay attention to " +
                    "\"${input.item.title}\"."
            }

            conflict?.type ==
                DynamicScheduleAttentionConflictType
                    .LEARNED_SUPPRESSION_VS_HARD_REQUIREMENT -> {

                "\"${input.item.title}\" is being surfaced " +
                    "because a required condition currently needs attention."
            }

            conflict?.type ==
                DynamicScheduleAttentionConflictType
                    .CONTEXT_MEMORY_CONFLICT -> {

                "\"${input.item.title}\" is more relevant " +
                    "in the current context than usual."
            }

            input.conflictResult.attentionRequired -> {

                "\"${input.item.title}\" is currently relevant."
            }

            else -> {

                "\"${input.item.title}\" does not currently require attention."
            }
        }
    }
}
