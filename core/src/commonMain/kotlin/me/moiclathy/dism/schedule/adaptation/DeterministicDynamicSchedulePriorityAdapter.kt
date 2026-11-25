package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

class DeterministicDynamicSchedulePriorityAdapter :
    DynamicSchedulePriorityAdapter {

    override fun adapt(
        input: DynamicSchedulePriorityAdaptationInput,
        policy: DynamicSchedulePriorityAdaptationPolicy,
    ): DynamicSchedulePriorityAdaptationResult {

        val adaptations = mutableListOf<DynamicSchedulePriorityAdaptation>()
        val diagnostics = mutableListOf<String>()

        input.model.items.forEach { item ->

            val history = input.histories[item.id.value]
                ?: run {
                    diagnostics +=
                        "No attention history for item ${item.id}"

                    adaptations +=
                        DynamicSchedulePriorityAdaptation(
                            itemId = item.id,
                            basePriority = item.priority,
                            adaptationFactor = 1.0,
                            effectivePriority = item.priority,
                            reasons = listOf(
                                "No attention history; priority unchanged",
                            ),
                        )

                    return@forEach
                }

            val factor = calculateFactor(
                history = history,
                policy = policy,
            )

            val effectivePriority =
                (item.priority * factor)
                    .coerceIn(0.0, 1.0)

            adaptations +=
                DynamicSchedulePriorityAdaptation(
                    itemId = item.id,
                    basePriority = item.priority,
                    adaptationFactor = factor,
                    effectivePriority = effectivePriority,
                    reasons = buildReasons(
                        history = history,
                        factor = factor,
                    ),
                )
        }

        return DynamicSchedulePriorityAdaptationResult(
            adaptations = adaptations,
            diagnostics = diagnostics,
        )
    }

    /**
     * 12.9：使用衰减后的聚合注意力历史进行优先级适配。
     *
     * 12.8 的 [adapt] 仍然保持不变；
     * 这里只是增加一条 decayed 通道，
     * 避免再创建第三个 Priority Adapter。
     */
    fun adaptDecayed(
        input: DynamicScheduleDecayedPriorityAdaptationInput,
        policy: DynamicSchedulePriorityAdaptationPolicy =
            DynamicSchedulePriorityAdaptationPolicy(),
    ): DynamicSchedulePriorityAdaptationResult {

        val adaptations =
            mutableListOf<DynamicSchedulePriorityAdaptation>()

        val diagnostics =
            mutableListOf<String>()

        input.model.items.forEach { item ->

            val history =
                input.histories[item.id.value]

            if (history == null) {

                diagnostics +=
                    "No decayed attention history for item ${item.id}"

                adaptations +=
                    DynamicSchedulePriorityAdaptation(
                        itemId = item.id,
                        basePriority = item.priority,
                        adaptationFactor = 1.0,
                        effectivePriority = item.priority,
                        reasons = listOf(
                            "No decayed attention history",
                        ),
                    )

                return@forEach
            }

            val factor =
                calculateFactor(
                    history = history,
                    policy = policy,
                )

            val effectivePriority =
                (item.priority * factor)
                    .coerceIn(0.0, 1.0)

            adaptations +=
                DynamicSchedulePriorityAdaptation(
                    itemId = item.id,
                    basePriority = item.priority,
                    adaptationFactor = factor,
                    effectivePriority = effectivePriority,
                    reasons = listOf(
                        "Priority adapted from decayed attention history",
                        "Effective historical evidence=${history.evidenceWeight}",
                    ),
                )
        }

        return DynamicSchedulePriorityAdaptationResult(
            adaptations = adaptations,
            diagnostics = diagnostics,
        )
    }

    /**
     * 12.9 decayed 通道使用的因子计算。
     */
    private fun calculateFactor(
        history: DynamicScheduleDecayedAttentionHistory,
        policy: DynamicSchedulePriorityAdaptationPolicy,
    ): Double {

        var factor = 1.0

        factor -=
            history.ignoredWeight *
                policy.ignoredPenalty

        factor -=
            history.dismissedWeight *
                policy.dismissedPenalty

        factor +=
            history.engagedWeight *
                policy.engagedBonus

        factor +=
            history.requestedWeight *
                policy.requestedBonus

        factor +=
            history.feedbackSignal *
                policy.feedbackWeight

        return factor.coerceIn(
            policy.minimumFactor,
            policy.maximumFactor,
        )
    }

    private fun calculateFactor(
        history: DynamicScheduleAttentionHistory,
        policy: DynamicSchedulePriorityAdaptationPolicy,
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
        history: DynamicScheduleAttentionHistory,
        factor: Double,
    ): List<String> {

        return buildList {

            if (history.ignoredCount > 0) {
                add(
                    "Previously ignored ${history.ignoredCount} time(s)",
                )
            }

            if (history.dismissedCount > 0) {
                add(
                    "Previously dismissed ${history.dismissedCount} time(s)",
                )
            }

            if (history.engagedCount > 0) {
                add(
                    "Previously engaged ${history.engagedCount} time(s)",
                )
            }

            if (history.requestedCount > 0) {
                add(
                    "Previously requested ${history.requestedCount} time(s)",
                )
            }

            if (history.feedbackSignal != 0.0) {
                add(
                    "Explicit attention feedback=" +
                        history.feedbackSignal,
                )
            }

            add(
                "Attention adaptation factor=$factor",
            )
        }
    }
}
