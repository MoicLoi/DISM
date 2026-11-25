package me.moiclathy.dism.context.semantics

import me.moiclathy.dism.context.model.ContextMode

/**
 * 一条 Context → Semantic 的规则。
 *
 * 例如：
 *
 * IN_CLASS + EVENT
 *     → RELEVANT
 *     → +0.30
 *
 * SLEEPING + REMINDER
 *     → SUPPRESSING
 *     → -0.60
 */
data class ContextSemanticRule(
    val contextMode: ContextMode,
    val targetType: String,
    val relation: ContextSemanticRelation,
    val weightDelta: Double,
    val reason: String,
) {

    init {
        require(targetType.isNotBlank()) {
            "targetType must not be blank."
        }

        require(weightDelta in -1.0..1.0) {
            "weightDelta must be between -1.0 and 1.0."
        }

        require(reason.isNotBlank()) {
            "reason must not be blank."
        }
    }
}
