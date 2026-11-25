package me.moiclathy.dism.nl.relation

import kotlin.uuid.Uuid

class DeterministicSemanticRelationCompiler : SemanticRelationCompiler {
    override fun compile(
        subjectId: Uuid,
        predicate: String,
        objectId: Uuid,
        confidence: Double,
        source: String?,
    ): SemanticRelationCandidate {
        val canonicalPredicate = predicate.trim().uppercase()
        require(canonicalPredicate.isNotBlank()) { "predicate must not be blank" }
        require(subjectId != objectId) { "subject and object must be different entities" }
        return SemanticRelationCandidate(
            subjectId = subjectId,
            predicate = canonicalPredicate,
            objectId = objectId,
            confidence = confidence.coerceIn(0.0, 1.0),
            source = source,
        )
    }
}
