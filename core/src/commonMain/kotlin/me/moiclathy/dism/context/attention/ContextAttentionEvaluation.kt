package me.moiclathy.dism.context.attention

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * Context → Attention 的完整计算结果。
 */
data class ContextAttentionEvaluation(
    val target: ContextAttentionTarget,
    val contextMode: ContextMode,
    val contextConfidence: Double,
    val priority: ContextAttentionPriority,
    val adjustment: ContextAttentionAdjustment,
    val evaluatedAt: Instant,
    val reason: String,
) {

    init {
        require(
            contextConfidence in 0.0..1.0
        ) {
            "contextConfidence must be between 0.0 and 1.0."
        }

        require(reason.isNotBlank()) {
            "reason must not be blank."
        }
    }
}
