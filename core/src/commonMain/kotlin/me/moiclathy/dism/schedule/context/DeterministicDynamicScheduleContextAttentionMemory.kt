package me.moiclathy.dism.schedule.context

import kotlin.math.max

class DeterministicDynamicScheduleContextAttentionMemory :
    DynamicScheduleContextAttentionMemory {

    private val histories =
        mutableMapOf<
            DynamicScheduleContextAttentionKey,
            DynamicScheduleContextAttentionHistory,
        >()

    override fun evaluate(
        input: DynamicScheduleContextAttentionInput,
        policy: DynamicScheduleContextAttentionPolicy,
    ): DynamicScheduleContextAttentionResult {

        val matched =
            input.activeContextKeys.mapNotNull { contextKey ->

                val key =
                    DynamicScheduleContextAttentionKey(
                        itemId = input.itemId,
                        contextKey = contextKey,
                    )

                histories[key]
                    ?: input.histories[key]
            }

        if (matched.isEmpty()) {
            return DynamicScheduleContextAttentionResult(
                itemId = input.itemId,
                baseFactor = 1.0,
                contextFactor = 1.0,
                effectiveFactor = 1.0,
                activeContextKeys =
                    input.activeContextKeys,
                matchedHistoryKeys =
                    emptyList(),
                reasons =
                    listOf(
                        "No context-specific attention history",
                    ),
            )
        }

        val contextFactors =
            matched.map { history ->

                calculateFactor(
                    history = history,
                    policy = policy,
                )
            }

        /*
         * Multiple contexts are combined conservatively.
         *
         * The strongest context signal is used instead of
         * multiplying all factors together.
         */
        val strongestFactor =
            contextFactors.maxOrNull() ?: 1.0

        val effectiveFactor =
            (
                1.0 +
                    (strongestFactor - 1.0) *
                    policy.contextInfluence
                )
                .coerceIn(
                    policy.minimumFactor,
                    policy.maximumFactor,
                )

        return DynamicScheduleContextAttentionResult(
            itemId = input.itemId,
            baseFactor = 1.0,
            contextFactor = strongestFactor,
            effectiveFactor = effectiveFactor,
            activeContextKeys =
                input.activeContextKeys,
            matchedHistoryKeys =
                matched.map { it.key },
            reasons =
                buildReasons(
                    matched = matched,
                    strongestFactor = strongestFactor,
                    effectiveFactor = effectiveFactor,
                ),
        )
    }

    override fun record(
        history: DynamicScheduleContextAttentionHistory,
    ) {
        histories[history.key] = history
    }

    private fun calculateFactor(
        history: DynamicScheduleContextAttentionHistory,
        policy: DynamicScheduleContextAttentionPolicy,
    ): Double {

        var factor = 1.0

        factor -=
            history.ignoredCount *
                policy.ignoredPenalty

        factor -=
            history.dismissedCount *
                policy.dismissedPenalty

        factor +=
            history.engagedCount *
                policy.engagedBonus

        factor +=
            history.requestedCount *
                policy.requestedBonus

        factor +=
            history.feedbackSignal *
                policy.feedbackWeight

        return factor.coerceIn(
            policy.minimumFactor,
            policy.maximumFactor,
        )
    }

    private fun buildReasons(
        matched:
            List<DynamicScheduleContextAttentionHistory>,
        strongestFactor: Double,
        effectiveFactor: Double,
    ): List<String> {

        return buildList {

            matched.forEach { history ->

                if (history.ignoredCount > 0) {
                    add(
                        "${history.key.contextKey}: " +
                            "ignored ${history.ignoredCount} time(s)",
                    )
                }

                if (history.dismissedCount > 0) {
                    add(
                        "${history.key.contextKey}: " +
                            "dismissed ${history.dismissedCount} time(s)",
                    )
                }

                if (history.engagedCount > 0) {
                    add(
                        "${history.key.contextKey}: " +
                            "engaged ${history.engagedCount} time(s)",
                    )
                }

                if (history.requestedCount > 0) {
                    add(
                        "${history.key.contextKey}: " +
                            "requested ${history.requestedCount} time(s)",
                    )
                }
            }

            add(
                "Strongest context factor=$strongestFactor",
            )

            add(
                "Effective context influence=$effectiveFactor",
            )
        }
    }
}
