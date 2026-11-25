package me.moiclathy.dism.attention.context

/** Result consumed by the existing AttentionEngine/renderer layer. */
data class AttentionContextDecision(
    val effectiveBudget: Int,
    val allowNonCritical: Boolean,
    val mode: AttentionContextMode,
)
