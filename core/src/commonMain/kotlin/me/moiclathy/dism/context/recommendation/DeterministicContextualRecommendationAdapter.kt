package me.moiclathy.dism.context.recommendation

/**
 * Contextual Recommendation 的确定性实现。
 *
 * 不负责：
 *
 * - Recommendation 创建
 * - Recommendation Lifecycle
 * - Attention
 * - Notification
 * - Persistence
 */
class DeterministicContextualRecommendationAdapter(
    private val maximumAdjustment: Double = 0.30,
    private val neutralSemanticWeight: Double = 0.50,
) : ContextualRecommendationAdapter {

    init {
        require(maximumAdjustment >= 0.0) {
            "maximumAdjustment must not be negative."
        }

        require(neutralSemanticWeight in 0.0..1.0) {
            "neutralSemanticWeight must be between 0.0 and 1.0."
        }
    }

    override fun evaluate(
        input: ContextualRecommendationInput,
    ): ContextualRecommendationResult {

        /*
         * Semantic Weight 相对于中性点的偏移。
         */
        val semanticDelta =
            input.contextualSemanticWeight -
                neutralSemanticWeight

        /*
         * Context Confidence 决定：
         *
         * “当前 Context 对 Recommendation
         * 的影响应该有多大？”
         */
        val confidenceAdjustedDelta =
            semanticDelta *
                input.contextConfidence

        /*
         * 把 [-0.5, +0.5]
         * 映射到 [-1, +1]。
         */
        val normalizedDelta =
            confidenceAdjustedDelta * 2.0

        /*
         * 限制 Context 对 Recommendation
         * 的最大影响。
         */
        val adjustment =
            normalizedDelta *
                maximumAdjustment

        val adjustedPriority =
            (
                input.basePriority +
                    adjustment
                ).coerceIn(
                    0.0,
                    1.0,
                )

        val actualDelta =
            adjustedPriority -
                input.basePriority

        return ContextualRecommendationResult(
            adjustment =
                ContextualRecommendationAdjustment(
                    basePriority =
                        input.basePriority,

                    adjustedPriority =
                        adjustedPriority,

                    delta =
                        actualDelta,

                    contextualSemanticWeight =
                        input.contextualSemanticWeight,

                    contextConfidence =
                        input.contextConfidence,

                    contextMode =
                        input.target.contextMode,
                ),

            evaluatedAt =
                input.evaluatedAt,
        )
    }
}
