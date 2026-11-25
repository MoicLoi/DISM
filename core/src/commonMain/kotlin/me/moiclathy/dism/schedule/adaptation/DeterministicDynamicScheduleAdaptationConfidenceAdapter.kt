package me.moiclathy.dism.schedule.adaptation

class DeterministicDynamicScheduleAdaptationConfidenceAdapter :
    DynamicScheduleAdaptationConfidenceAdapter {

    override fun calculate(
        input:
            DynamicScheduleAdaptationConfidenceInput,

        policy:
            DynamicScheduleAdaptationConfidencePolicy,
    ): DynamicScheduleAdaptationConfidence {

        val historicalEvidence =
            input.historicalEvidence
                .coerceIn(0.0, 1.0)

        val contextEvidence =
            calculateContextEvidence(
                input.contextInfluence,
            )

        val stabilityEvidence =
            input.stabilityEvidence
                .coerceIn(0.0, 1.0)

        val sampleEvidence =
            calculateSampleEvidence(
                sampleCount =
                    input.sampleCount,

                saturation =
                    policy.sampleSaturation,
            )

        val confidence =
            (
                historicalEvidence *
                    policy.normalizedHistoricalWeight
                +
                contextEvidence *
                    policy.normalizedContextWeight
                +
                stabilityEvidence *
                    policy.normalizedStabilityWeight
                +
                sampleEvidence *
                    policy.normalizedSampleWeight
                )
                .coerceIn(0.0, 1.0)

        val enabled =
            input.priorityAdaptation
                .adaptationFactor
                .isFinite() &&
                confidence >=
                    policy.minimumConfidence

        return DynamicScheduleAdaptationConfidence(
            itemId =
                input.priorityAdaptation.itemId,

            value =
                confidence,

            historicalEvidence =
                historicalEvidence,

            contextEvidence =
                contextEvidence,

            stabilityEvidence =
                stabilityEvidence,

            sampleEvidence =
                sampleEvidence,

            enabled =
                enabled,

            reasons =
                buildReasons(
                    confidence =
                        confidence,

                    historicalEvidence =
                        historicalEvidence,

                    contextEvidence =
                        contextEvidence,

                    stabilityEvidence =
                        stabilityEvidence,

                    sampleEvidence =
                        sampleEvidence,

                    enabled =
                        enabled,
                ),
        )
    }

    private fun calculateContextEvidence(
        influence:
            me.moiclathy.dism.schedule.context
                .DynamicScheduleContextPriorityInfluence?,
    ): Double {

        if (influence == null) {
            /*
             * 没有 Context 证据。
             *
             * 不能解释成“Context 完全不可信”。
             * 第一版采取中性值。
             */
            return 0.5
        }

        if (influence.excluded) {
            /*
             * EXCLUDED 是非常明确的 Context 信号。
             */
            return 1.0
        }

        if (!influence.requiredSatisfied) {
            /*
             * Required missing 同样是明确的上下文状态。
             */
            return 1.0
        }

        return influence.contextMatch
            .coerceIn(0.0, 1.0)
    }

    private fun calculateSampleEvidence(
        sampleCount: Int,
        saturation: Double,
    ): Double {

        if (sampleCount <= 0) {
            return 0.0
        }

        /*
         * 使用简单的饱和曲线：
         *
         * 1 sample  -> 0.10
         * 5 samples -> 0.50
         * 10 samples -> 1.00
         *
         * 超过 saturation 不继续增长。
         */
        return (
            sampleCount.toDouble() /
                saturation
            )
            .coerceIn(0.0, 1.0)
    }

    private fun buildReasons(
        confidence: Double,
        historicalEvidence: Double,
        contextEvidence: Double,
        stabilityEvidence: Double,
        sampleEvidence: Double,
        enabled: Boolean,
    ): List<String> {

        return buildList {

            add(
                "Historical evidence=$historicalEvidence",
            )

            add(
                "Context evidence=$contextEvidence",
            )

            add(
                "Stability evidence=$stabilityEvidence",
            )

            add(
                "Sample evidence=$sampleEvidence",
            )

            add(
                "Adaptation confidence=$confidence",
            )

            if (enabled) {
                add(
                    "Adaptation confidence threshold satisfied",
                )
            } else {
                add(
                    "Adaptation confidence threshold not satisfied",
                )
            }
        }
    }
}
