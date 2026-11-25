package me.moiclathy.dism.context.semantics

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * 一次 Context-dependent Semantic Evaluation。
 */
data class ContextSemanticEvaluation(
    val target: ContextSemanticTarget,
    val contextMode: ContextMode,
    val contextConfidence: Double,
    val score: ContextSemanticScore,
    val matchedRules: List<ContextSemanticRule> = emptyList(),
    val evaluatedAt: Instant,
) {

    init {
        require(
            contextConfidence in 0.0..1.0
        ) {
            "contextConfidence must be between 0.0 and 1.0."
        }
    }
}
