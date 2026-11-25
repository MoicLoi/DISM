package me.moiclathy.dism.context.attention.runtime

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context 对 Attention Priority 的最终运行时调整。
 */
data class ContextAttentionRuntimeAdjustment(
    val basePriority: Double,

    val contextualPriority: Double,

    val delta: Double,

    val contextMode: ContextMode,

    val contextConfidence: Double,
) {

    init {
        require(basePriority in 0.0..1.0) {
            "basePriority must be between 0.0 and 1.0."
        }

        require(contextualPriority in 0.0..1.0) {
            "contextualPriority must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }
    }
}
