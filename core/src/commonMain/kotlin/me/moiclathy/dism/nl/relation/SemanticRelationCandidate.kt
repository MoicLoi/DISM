package me.moiclathy.dism.nl.relation

import kotlin.uuid.Uuid

/** A resolved semantic relation ready for compilation into the domain RelationKey. */
data class SemanticRelationCandidate(
    val subjectId: Uuid,
    val predicate: String,
    val objectId: Uuid,
    val confidence: Double,
    val source: String? = null,
) {
    init {
        require(predicate.isNotBlank()) { "predicate must not be blank" }
        require(confidence in 0.0..1.0) { "confidence must be in [0, 1]" }
    }
}
