package me.moiclathy.dism.context.recommendation

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context 对 Recommendation Priority 的调整。
 */
data class ContextualRecommendationAdjustment(
    val basePriority: Double,

    val adjustedPriority: Double,

    val delta: Double,

    val contextualSemanticWeight: Double,

    val contextConfidence: Double,

    val contextMode: ContextMode,
) {

    init {
        require(basePriority in 0.0..1.0) {
            "basePriority must be between 0.0 and 1.0."
        }

        require(adjustedPriority in 0.0..1.0) {
            "adjustedPriority must be between 0.0 and 1.0."
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
