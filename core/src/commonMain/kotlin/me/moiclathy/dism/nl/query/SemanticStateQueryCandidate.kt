package me.moiclathy.dism.nl.query

/** Candidate emitted by the NL layer; it never mutates semantic state. */
data class SemanticStateQueryCandidate(
    val query: SemanticStateQuery,
    val unresolvedMentions: List<String> = emptyList(),
    val rationale: String = "",
)
