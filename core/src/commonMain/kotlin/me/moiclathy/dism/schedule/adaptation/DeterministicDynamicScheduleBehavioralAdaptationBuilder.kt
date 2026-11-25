package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.3
 *
 * 第一版 Behavioral Adaptation Model。
 *
 * **完全由 14.2 Interpretation 决定**：
 *
 * ```text
 * 不访问数据库
 * 不修改 Schedule
 * 不修改 Attention
 * 不修改 Priority
 * 不产生随机结果
 * ```
 */
class DeterministicDynamicScheduleBehavioralAdaptationBuilder :
    DynamicScheduleBehavioralAdaptationBuilder {

    override fun build(
        itemId: DynamicScheduleItemId,

        interpretation:
            DynamicScheduleDecisionInterpretation,
    ): DynamicScheduleBehavioralAdaptation {

        val completion =
            interpretation.find(
                DynamicScheduleDecisionInterpretationType
                    .COMPLETION_TENDENCY,
            )

        val postponement =
            interpretation.find(
                DynamicScheduleDecisionInterpretationType
                    .POSTPONEMENT_TENDENCY,
            )

        val cancellation =
            interpretation.find(
                DynamicScheduleDecisionInterpretationType
                    .CANCELLATION_TENDENCY,
            )

        val highStability =
            interpretation.find(
                DynamicScheduleDecisionInterpretationType
                    .HIGH_DECISION_STABILITY,
            )

        val lowStability =
            interpretation.find(
                DynamicScheduleDecisionInterpretationType
                    .LOW_DECISION_STABILITY,
            )

        val recentlyActive =
            interpretation.find(
                DynamicScheduleDecisionInterpretationType
                    .RECENTLY_ACTIVE,
            )

        val notRecentlyActive =
            interpretation.find(
                DynamicScheduleDecisionInterpretationType
                    .NOT_RECENTLY_ACTIVE,
            )

        /*
         * 双向语义在这里被还原成**有方向的数值**。
         *
         * LOW_DECISION_STABILITY 的强度是补值，
         * 所以 1 - strength 得到真正的 stability。
         */
        val decisionStability =
            when {
                highStability != null ->
                    highStability.strength

                lowStability != null ->
                    1.0 - lowStability.strength

                else -> 0.0
            }

        val recentActivity =
            when {
                recentlyActive != null ->
                    recentlyActive.strength

                notRecentlyActive != null ->
                    1.0 - notRecentlyActive.strength

                else -> 0.0
            }

        val signals =
            listOfNotNull(
                completion,
                postponement,
                cancellation,
                highStability,
                lowStability,
                recentlyActive,
                notRecentlyActive,
            )

        return DynamicScheduleBehavioralAdaptation(
            itemId = itemId,

            completionTendency =
                completion?.strength ?: 0.0,

            postponementTendency =
                postponement?.strength ?: 0.0,

            cancellationTendency =
                cancellation?.strength ?: 0.0,

            decisionStability = decisionStability,

            recentActivity = recentActivity,

            confidence = calculateConfidence(signals),

            sampleCount =
                signals.maxOfOrNull { it.sampleCount } ?: 0,
        )
    }

    /**
     * 按**样本量加权**的平均置信度。
     *
     * ### 为什么不能简单平均（14.3 §4）
     *
     * ```text
     * Feature A: confidence = 1.0, sampleCount = 1
     * Feature B: confidence = 0.8, sampleCount = 20
     * ```
     *
     * 简单平均得到 0.9，等于让"一次观察"和"二十次观察"
     * 拥有同样的影响力。
     *
     * 加权之后会明显更偏向样本多的那一侧。
     *
     * 注意这仍然只是**模型置信度**，
     * 没有被偷偷当成 Priority 权重。
     */
    private fun calculateConfidence(
        signals:
            List<DynamicScheduleDecisionInterpretationSignal>,
    ): Double {

        if (signals.isEmpty()) {
            return 0.0
        }

        val totalWeight =
            signals.sumOf {
                it.sampleCount
                    .coerceAtLeast(1)
                    .toDouble()
            }

        if (totalWeight <= 0.0) {
            return 0.0
        }

        val weightedConfidence =
            signals.sumOf { signal ->
                val weight =
                    signal.sampleCount
                        .coerceAtLeast(1)
                        .toDouble()

                signal.confidence * weight
            } / totalWeight

        return weightedConfidence.coerceIn(0.0, 1.0)
    }
}
