package me.moiclathy.dism.context.recommendation

import kotlin.time.Instant

/**
 * Contextual Recommendation Runtime 输入。
 */
data class ContextualRecommendationInput(
    val target: ContextualRecommendationTarget,

    /**
     * Recommendation 原始优先级。
     */
    val basePriority: Double,

    /**
     * ⑨.⑫ Context → Semantic Runtime
     * 产生的上下文语义权重。
     */
    val contextualSemanticWeight: Double,

    /**
     * 当前 Context 的可信度。
     */
    val contextConfidence: Double,

    /**
     * 计算时间。
     */
    val evaluatedAt: Instant,
) {

    init {
        require(basePriority in 0.0..1.0) {
            "basePriority must be between 0.0 and 1.0."
        }

        require(
            contextualSemanticWeight in 0.0..1.0
        ) {
            "contextualSemanticWeight must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }
    }
}
