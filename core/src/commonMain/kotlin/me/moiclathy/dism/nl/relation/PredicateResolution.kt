package me.moiclathy.dism.nl.relation

import me.moiclathy.dism.domain.relation.Predicate

data class PredicateResolution(
    val predicate: Predicate,
    val confidence: Double,
    val reason: PredicateResolutionReason,
) {
    init { require(confidence in 0.0..1.0) { "confidence must be in [0,1]" } }
}

enum class PredicateResolutionReason {
    EXACT_CANONICAL,
    NORMALIZED_CANONICAL,
    CHINESE_ALIAS,
}
