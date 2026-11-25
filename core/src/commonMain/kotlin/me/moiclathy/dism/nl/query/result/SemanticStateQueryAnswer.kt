package me.moiclathy.dism.nl.query.result

import me.moiclathy.dism.nl.query.SemanticStateQuery

/** Structured, read-only answer produced from a resolved semantic state query. */
data class SemanticStateQueryAnswer(
    val query: SemanticStateQuery,
    val status: SemanticStateQueryAnswerStatus,
    val values: List<String> = emptyList(),
    val confidence: Double = 1.0,
    val source: String? = null,
)
