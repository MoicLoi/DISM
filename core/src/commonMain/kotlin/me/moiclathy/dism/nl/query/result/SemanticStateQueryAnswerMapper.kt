package me.moiclathy.dism.nl.query.result

import me.moiclathy.dism.nl.query.SemanticStateQuery
import me.moiclathy.dism.nl.query.adapter.SemanticStateQueryStateResult
import me.moiclathy.dism.nl.query.adapter.SemanticStateQueryValue

/** Maps the execution-layer result into a stable answer DTO without generating prose. */
class SemanticStateQueryAnswerMapper {
    fun map(query: SemanticStateQuery, result: SemanticStateQueryStateResult): SemanticStateQueryAnswer {
        return SemanticStateQueryAnswer(
            query = query,
            status = result.toAnswerStatus(),
            values = result.values.mapNotNull { it.displayValue() },
            confidence = result.confidence,
        )
    }

    private fun SemanticStateQueryStateResult.toAnswerStatus(): SemanticStateQueryAnswerStatus = when {
        !found -> SemanticStateQueryAnswerStatus.NOT_FOUND
        values.size > 1 -> SemanticStateQueryAnswerStatus.AMBIGUOUS
        else -> SemanticStateQueryAnswerStatus.ANSWERED
    }

    private fun SemanticStateQueryValue.displayValue(): String? =
        objectName ?: objectId ?: subjectId
}
