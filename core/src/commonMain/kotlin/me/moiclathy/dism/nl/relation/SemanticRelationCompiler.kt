package me.moiclathy.dism.nl.relation

/**
 * Compiles resolved mentions + a resolved predicate into a stable relation candidate.
 * It does not mutate RelationState or the SemanticStateEngine.
 */
interface SemanticRelationCompiler {
    fun compile(
        subjectId: kotlin.uuid.Uuid,
        predicate: String,
        objectId: kotlin.uuid.Uuid,
        confidence: Double,
        source: String? = null,
    ): SemanticRelationCandidate
}
