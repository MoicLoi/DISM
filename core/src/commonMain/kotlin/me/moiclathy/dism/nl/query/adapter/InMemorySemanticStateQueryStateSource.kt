package me.moiclathy.dism.nl.query.adapter

import me.moiclathy.dism.nl.query.SemanticStateQuery

/** Small deterministic fixture source for tests; not a replacement for the production state engine. */
class InMemorySemanticStateQueryStateSource(
    private val values: List<SemanticStateQueryValue>,
) : SemanticStateQueryStateSource {
    override fun query(query: SemanticStateQuery): SemanticStateQueryStateResult {
        val matches = values.filter { value ->
            query.subjectEntityId == null || value.subjectId == query.subjectEntityId
        }.filter { value ->
            query.predicate == null || value.predicate == query.predicate
        }
        return SemanticStateQueryStateResult(
            found = matches.isNotEmpty(),
            values = matches,
            confidence = matches.maxOfOrNull { it.confidence } ?: 0.0,
        )
    }
}
