package me.moiclathy.dism.nl.query.response

import me.moiclathy.dism.nl.query.result.SemanticStateQueryAnswer

/** Structured, presentation-independent response produced from a query answer. */
data class SemanticQueryResponse(
    val status: SemanticQueryResponseStatus,
    val text: String,
    val answer: SemanticStateQueryAnswer,
    val confidence: Double? = null,
)

enum class SemanticQueryResponseStatus {
    ANSWERED,
    NOT_FOUND,
    AMBIGUOUS,
    UNAVAILABLE,
}
