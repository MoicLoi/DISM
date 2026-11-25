package me.moiclathy.dism.context.attention

/**
 * Context 对 Attention Priority 的调整。
 */
data class ContextAttentionAdjustment(
    val basePriority: Double,
    val semanticWeight: Double,
    val contextConfidence: Double,
    val adjustment: Double,
    val effectivePriority: Double,
) {

    init {
        require(basePriority in 0.0..1.0) {
            "basePriority must be between 0.0 and 1.0."
        }

        require(semanticWeight in 0.0..1.0) {
            "semanticWeight must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }

        require(adjustment in -1.0..1.0) {
            "adjustment must be between -1.0 and 1.0."
        }

        require(effectivePriority in 0.0..1.0) {
            "effectivePriority must be between 0.0 and 1.0."
        }
    }
}
