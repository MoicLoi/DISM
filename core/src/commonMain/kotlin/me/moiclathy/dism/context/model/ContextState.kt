package me.moiclathy.dism.context.model

import kotlin.time.Instant

/** Current projected context; it contains no policy or side effects. */
data class ContextState(
    val contextId: ContextId,
    val mode: ContextMode = ContextMode.NORMAL,
    val confidence: Double = 1.0,
    val observedAt: Instant,
    val sourceClaimIds: List<String> = emptyList(),
) {
    init { require(confidence in 0.0..1.0) { "confidence must be in [0,1]" } }
}
