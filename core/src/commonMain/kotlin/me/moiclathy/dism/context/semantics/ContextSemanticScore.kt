package me.moiclathy.dism.context.semantics

/**
 * Context 对目标语义产生的最终权重。
 */
data class ContextSemanticScore(
    val baseWeight: Double,
    val contextDelta: Double,
    val effectiveWeight: Double,
    val relation: ContextSemanticRelation,
) {

    init {
        require(baseWeight in 0.0..1.0) {
            "baseWeight must be between 0.0 and 1.0."
        }

        require(contextDelta in -1.0..1.0) {
            "contextDelta must be between -1.0 and 1.0."
        }

        require(effectiveWeight in 0.0..1.0) {
            "effectiveWeight must be between 0.0 and 1.0."
        }
    }
}
