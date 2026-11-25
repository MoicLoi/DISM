package me.moiclathy.dism.context.e2e

/**
 * Context → Semantic → Recommendation → Attention
 * 完整 E2E Trace。
 *
 * Trace 是不可变的。
 */
data class ContextSemanticAttentionE2ETrace(
    val steps: List<ContextSemanticAttentionE2EStep> = emptyList(),

    val attentionKey: String,

    val basePriority: Double,

    val contextualSemanticWeight: Double,

    val contextualRecommendationPriority: Double,

    val attentionPriority: Double,

    val contextConfidence: Double,
) {

    init {
        require(attentionKey.isNotBlank()) {
            "attentionKey must not be blank."
        }

        require(basePriority in 0.0..1.0) {
            "basePriority must be between 0.0 and 1.0."
        }

        require(
            contextualSemanticWeight in 0.0..1.0
        ) {
            "contextualSemanticWeight must be between 0.0 and 1.0."
        }

        require(
            contextualRecommendationPriority in 0.0..1.0
        ) {
            "contextualRecommendationPriority must be between 0.0 and 1.0."
        }

        require(attentionPriority in 0.0..1.0) {
            "attentionPriority must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }
    }

    val successful: Boolean
        get() =
            steps.isNotEmpty() &&
                steps.all { it.successful }
}
