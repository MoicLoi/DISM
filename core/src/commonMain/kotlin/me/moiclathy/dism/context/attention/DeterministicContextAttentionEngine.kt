package me.moiclathy.dism.context.attention

import kotlin.math.abs
import kotlin.time.Instant

/**
 * 确定性的 Context → Attention Engine。
 */
class DeterministicContextAttentionEngine :
    ContextAttentionEngine {

    override fun evaluate(
        target: ContextAttentionTarget,
        basePriority: Double,
        semanticWeight: Double,
        contextConfidence: Double,
        now: Instant,
        policy: ContextAttentionPolicy,
    ): ContextAttentionEvaluation {

        require(basePriority in 0.0..1.0) {
            "basePriority must be between 0.0 and 1.0."
        }

        require(semanticWeight in 0.0..1.0) {
            "semanticWeight must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }

        /*
         * Semantic Weight 相对于中性点的偏移。
         *
         * 例如：
         *
         * 0.80 - 0.50 = +0.30
         * 0.20 - 0.50 = -0.30
         */
        val semanticDelta =
            semanticWeight -
                policy.neutralSemanticWeight

        /*
         * Confidence 决定这个 Context
         * 当前能够影响 Attention 多少。
         */
        val confidenceAdjustedDelta =
            semanticDelta *
                contextConfidence *
                policy.confidenceMultiplier

        /*
         * 将 semantic delta 映射到
         * Attention priority 的最大调整范围。
         *
         * semanticDelta ∈ [-0.5, +0.5]
         *
         * maximumAdjustment 控制实际影响大小。
         */
        val normalizedDelta =
            confidenceAdjustedDelta * 2.0

        val adjustment =
            (
                normalizedDelta *
                    policy.maximumAdjustment
                ).coerceIn(
                -policy.maximumAdjustment,
                policy.maximumAdjustment,
            )

        val effectivePriority =
            (
                basePriority + adjustment
                ).coerceIn(
                policy.minimumPriority,
                policy.maximumPriority,
            )

        val relation =
            when {
                abs(adjustment) < 0.000001 ->
                    "Context has neutral attention impact."

                adjustment > 0.0 ->
                    "Context increases attention priority."

                else ->
                    "Context decreases attention priority."
            }

        return ContextAttentionEvaluation(
            target = target,
            contextMode =
                target.contextMode,
            contextConfidence =
                contextConfidence,
            priority =
                ContextAttentionPriority(
                    value = effectivePriority,
                    adjustment = adjustment,
                ),
            adjustment =
                ContextAttentionAdjustment(
                    basePriority = basePriority,
                    semanticWeight = semanticWeight,
                    contextConfidence =
                        contextConfidence,
                    adjustment = adjustment,
                    effectivePriority =
                        effectivePriority,
                ),
            evaluatedAt = now,
            reason = relation,
        )
    }
}
