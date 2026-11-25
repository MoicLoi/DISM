package me.moiclathy.dism.nl.relation

import me.moiclathy.dism.domain.relation.Predicate

data class RelationResolutionResult(
    val mention: RelationMention,
    val predicate: Predicate?,
    val confidence: Double,
    val reason: PredicateResolutionReason? = null,
) {
    init { require(confidence in 0.0..1.0) { "confidence must be in [0,1]" } }
    val resolved: Boolean get() = predicate != null
}
