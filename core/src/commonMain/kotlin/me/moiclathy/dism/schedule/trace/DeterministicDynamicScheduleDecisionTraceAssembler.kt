package me.moiclathy.dism.schedule.trace

/**
 * 13.2
 *
 * Trace 组装器只消费 [DynamicScheduleDecisionTraceEvidence]。
 *
 * 它不再 import `schedule.runtime.*`，
 * 因此 Trace 与 Runtime 之间不再有循环依赖。
 */
class DeterministicDynamicScheduleDecisionTraceAssembler :
    DynamicScheduleDecisionTraceAssembler {

    override fun assemble(
        input: DynamicScheduleDecisionTraceInput,
    ): DynamicScheduleDecisionTrace {

        val evidence =
            input.evidence

        val item =
            evidence.item

        var trace =
            DynamicScheduleDecisionTrace(
                itemId = item.id,
            )

        /*
         * ---------------------------------------------------------
         * INPUT
         * ---------------------------------------------------------
         */

        trace =
            trace.withEntry(
                stage =
                    DynamicScheduleDecisionTraceStage.INPUT,

                message =
                    "Schedule item entered the intelligence runtime.",

                values =
                    mapOf(
                        "itemId" to
                            item.id.value,

                        "title" to
                            item.title,

                        "state" to
                            item.state.name,

                        "priority" to
                            item.priority.toString(),

                        "confidence" to
                            item.confidence.toString(),
                    ),
            )

        /*
         * ---------------------------------------------------------
         * DEPENDENCY_RESOLUTION
         * ---------------------------------------------------------
         */

        val dependencyMatches =
            evidence
                .dependencyResult
                .matches
                .filter {
                    it.dependency.toItemId ==
                        item.id
                }

        val blockingDependencies =
            dependencyMatches
                .filter {
                    it.blocking
                }

        val unsatisfiedDependencies =
            blockingDependencies
                .filterNot {
                    it.satisfied
                }

        trace =
            trace.withEntry(
                stage =
                    DynamicScheduleDecisionTraceStage
                        .DEPENDENCY_RESOLUTION,

                message =
                    "Schedule dependencies were evaluated.",

                values =
                    mapOf(
                        "matchCount" to
                            dependencyMatches
                                .size
                                .toString(),

                        "blockingCount" to
                            blockingDependencies
                                .size
                                .toString(),

                        "unsatisfiedCount" to
                            unsatisfiedDependencies
                                .size
                                .toString(),
                    ),

                decisive =
                    unsatisfiedDependencies
                        .isNotEmpty(),
            )

        /*
         * ---------------------------------------------------------
         * CONTEXT_BINDING
         * ---------------------------------------------------------
         */

        val contextMatch =
            evidence
                .contextResult
                .matches
                .firstOrNull {
                    it.itemId ==
                        item.id
                }

        if (contextMatch != null) {

            trace =
                trace.withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .CONTEXT_BINDING,

                    message =
                        "Current context was evaluated against schedule bindings.",

                    values =
                        mapOf(
                            "requiredSatisfied" to
                                contextMatch
                                    .requiredSatisfied
                                    .toString(),

                            "excludedSatisfied" to
                                contextMatch
                                    .excludedSatisfied
                                    .toString(),

                            "preferredScore" to
                                contextMatch
                                    .preferredScore
                                    .toString(),

                            "matchedContextCount" to
                                contextMatch
                                    .matchedContextKeys
                                    .size
                                    .toString(),

                            "missingRequiredCount" to
                                contextMatch
                                    .unmatchedRequiredContextKeys
                                    .size
                                    .toString(),
                        ),

                    decisive =
                        !contextMatch.contextSatisfied,
                )

        } else {

            trace =
                trace.withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .CONTEXT_BINDING,

                    message =
                        "No explicit context binding matched this item.",

                    values =
                        mapOf(
                            "matched" to "false",
                        ),
                )
        }

        /*
         * ---------------------------------------------------------
         * ATTENTION_MEMORY
         *
         * 当前实际 Runtime 中：
         *
         * Context Attention Memory
         *        ↓
         * applyAttentionPriority()
         *
         * 因此这里记录当前 item 最终进入 projection 前的
         * schedule priority。
         * ---------------------------------------------------------
         */

        trace =
            trace.withEntry(
                stage =
                    DynamicScheduleDecisionTraceStage
                        .ATTENTION_MEMORY,

                message =
                    "Attention memory influenced the schedule priority.",

                values =
                    mapOf(
                        "priority" to
                            item.priority.toString(),
                    ),
            )

        /*
         * ---------------------------------------------------------
         * PRIORITY_ADAPTATION
         * ---------------------------------------------------------
         */

        val adaptation =
            evidence
                .priorityAdaptation
                .adaptations
                .firstOrNull {
                    it.itemId ==
                        item.id
                }

        if (adaptation != null) {

            trace =
                trace.withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .PRIORITY_ADAPTATION,

                    message =
                        "Historical attention behavior adapted schedule priority.",

                    values =
                        mapOf(
                            "basePriority" to
                                adaptation
                                    .basePriority
                                    .toString(),

                            "effectivePriority" to
                                adaptation
                                    .effectivePriority
                                    .toString(),

                            "adaptationFactor" to
                                adaptation
                                    .adaptationFactor
                                    .toString(),
                        ),
                )

        } else {

            trace =
                trace.withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .PRIORITY_ADAPTATION,

                    message =
                        "No historical priority adaptation was applied.",

                    values =
                        mapOf(
                            "effectivePriority" to
                                item.priority
                                    .toString(),
                        ),
                )
        }

        /*
         * ---------------------------------------------------------
         * ATTENTION_PROJECTION
         * ---------------------------------------------------------
         */

        val projected =
            evidence
                .attentionProjection
                .candidates
                .firstOrNull {
                    it.itemId ==
                        item.id
                }

        trace =
            trace.withEntry(
                stage =
                    DynamicScheduleDecisionTraceStage
                        .ATTENTION_PROJECTION,

                message =
                    if (projected != null) {
                        "Schedule item was projected into Attention."
                    } else {
                        "Schedule item was not projected into Attention."
                    },

                values =
                    mapOf(
                        "projected" to
                            (projected != null)
                                .toString(),

                        "schedulePriority" to
                            (
                                projected
                                    ?.schedulePriority
                                    ?: 0.0
                            ).toString(),

                        "scheduleConfidence" to
                            (
                                projected
                                    ?.scheduleConfidence
                                    ?: 0.0
                            ).toString(),
                    ),

                decisive =
                    projected == null,
            )

        /*
         * ---------------------------------------------------------
         * CONFLICT_RESOLUTION
         *
         * 当前如果还没有真正接入 Conflict Resolver，
         * 就不伪造一次 Conflict。
         *
         * 但 Trace 已经保留这个 stage。
         * ---------------------------------------------------------
         */

        if (input.conflictMessage != null) {

            trace =
                trace.withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .CONFLICT_RESOLUTION,

                    message =
                        input.conflictMessage,

                    decisive =
                        input.conflictDecisive,
                )
        }

        /*
         * ---------------------------------------------------------
         * ATTENTION_SELECTION
         * ---------------------------------------------------------
         */

        /*
         * 13.2：
         *
         * Trace 不再访问 AttentionRuntime。
         * Runtime 已经把"哪些 key 被选中 / 抑制"算好放进 Evidence，
         * 这里只做集合成员判断。
         */
        val selected =
            projected
                ?.candidate
                ?.recommendation
                ?.key
                ?.let {
                    it in evidence.attentionSelectedKeys
                }
                ?: false

        val suppressed =
            projected
                ?.candidate
                ?.recommendation
                ?.key
                ?.let {
                    it in evidence.attentionSuppressedKeys
                }
                ?: false

        if (projected != null) {

            trace =
                trace.withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .ATTENTION_SELECTION,

                    message =
                        when {
                            selected ->
                                "Attention candidate was selected."

                            suppressed ->
                                "Attention candidate was suppressed."

                            else ->
                                "Attention candidate was not present in the final selection."
                        },

                    values =
                        mapOf(
                            "selected" to
                                selected.toString(),

                            "suppressed" to
                                suppressed.toString(),

                            "priority" to
                                projected
                                    .candidate
                                    .recommendation
                                    .priority
                                    .toString(),
                        ),

                    decisive = true,
                )
        }

        /*
         * ---------------------------------------------------------
         * EXPLANATION
         * ---------------------------------------------------------
         */

        if (input.explanationMessage != null) {

            trace =
                trace.withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .EXPLANATION,

                    message =
                        input.explanationMessage,
                )
        }

        /*
         * ---------------------------------------------------------
         * OUTPUT
         * ---------------------------------------------------------
         */

        trace =
            trace.withEntry(
                stage =
                    DynamicScheduleDecisionTraceStage
                        .OUTPUT,

                message =
                    "Dynamic Schedule Intelligence produced a decision.",

                values =
                    mapOf(
                        "selected" to
                            selected.toString(),

                        "projected" to
                            (projected != null)
                                .toString(),

                        "scheduleRevision" to
                            evidence.scheduleRevision
                                .toString(),

                        "timestamp" to
                            evidence.now
                                .toString(),
                    ),
            )

        return trace
    }
}
