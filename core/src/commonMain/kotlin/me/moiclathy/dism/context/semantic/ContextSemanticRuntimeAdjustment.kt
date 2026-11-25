package me.moiclathy.dism.context.semantic

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context 对 Semantic Weight 的运行时调整。
 *
 * 注意：
 *
 * 这不是新的 Semantic State。
 *
 * 它只是：
 *
 * Semantic State
 *      +
 * Context
 *      ↓
 * Runtime Adjustment
 */
data class ContextSemanticRuntimeAdjustment(
    val baseWeight: Double,

    val adjustedWeight: Double,

    val delta: Double,

    val contextMode: ContextMode,

    val contextConfidence: Double,
) {

    init {
        require(baseWeight in 0.0..1.0) {
            "baseWeight must be between 0.0 and 1.0."
        }

        require(adjustedWeight in 0.0..1.0) {
            "adjustedWeight must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }
    }
}
