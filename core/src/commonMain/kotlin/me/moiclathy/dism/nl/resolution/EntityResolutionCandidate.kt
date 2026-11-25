package me.moiclathy.dism.nl.resolution

import me.moiclathy.dism.domain.entity.Entity

data class EntityResolutionCandidate(
    val entity: Entity,
    val score: Double,
    val reason: EntityResolutionReason,
) {
    init { require(score in 0.0..1.0) { "score must be in [0,1]" } }
}

enum class EntityResolutionReason {
    EXACT_CANONICAL_NAME,
    EXACT_ALIAS,
    NORMALIZED_CANONICAL_NAME,
    NORMALIZED_ALIAS,
    TYPE_COMPATIBLE_NAME,
}
