package me.moiclathy.dism.attention.explanation

/**
 * Human-readable, structured explanation of one attention decision.
 * This describes policy reasoning only; it does not expose internal model details.
 */
data class AttentionDecisionExplanation(
    val selected: Boolean,
    val basePriority: Double,
    val effectivePriority: Double,
    val reasons: List<AttentionDecisionReason> = emptyList(),
    val summary: String = "",
) {
    init {
        require(basePriority in 0.0..1.0)
        require(effectivePriority in 0.0..1.0)
    }
}
