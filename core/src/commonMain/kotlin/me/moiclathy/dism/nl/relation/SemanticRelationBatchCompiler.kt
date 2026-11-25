package me.moiclathy.dism.nl.relation

/** Deterministic batch compilation: stable input order and duplicate removal. */
class SemanticRelationBatchCompiler(
    private val compiler: SemanticRelationCompiler = DeterministicSemanticRelationCompiler(),
) {
    fun compileAll(inputs: List<Input>): List<SemanticRelationCandidate> {
        val result = LinkedHashMap<Triple<kotlin.uuid.Uuid, String, kotlin.uuid.Uuid>, SemanticRelationCandidate>()
        inputs.forEach { input ->
            val candidate = compiler.compile(
                subjectId = input.subjectId,
                predicate = input.predicate,
                objectId = input.objectId,
                confidence = input.confidence,
                source = input.source,
            )
            val key = Triple(candidate.subjectId, candidate.predicate, candidate.objectId)
            val existing = result[key]
            if (existing == null || candidate.confidence > existing.confidence) result[key] = candidate
        }
        return result.values.toList()
    }

    data class Input(
        val subjectId: kotlin.uuid.Uuid,
        val predicate: String,
        val objectId: kotlin.uuid.Uuid,
        val confidence: Double,
        val source: String? = null,
    )
}
