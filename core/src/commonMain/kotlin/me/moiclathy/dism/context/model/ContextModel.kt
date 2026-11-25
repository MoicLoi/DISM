package me.moiclathy.dism.context.model

import kotlin.time.Instant

/** Top-level immutable situation snapshot used by later context resolution stages. */
data class ContextModel(
    val now: Instant,
    val states: Map<ContextId, ContextState> = emptyMap(),
    val activeClaims: List<ContextClaim> = emptyList(),
) {
    fun state(contextId: ContextId): ContextState? = states[contextId]
}
