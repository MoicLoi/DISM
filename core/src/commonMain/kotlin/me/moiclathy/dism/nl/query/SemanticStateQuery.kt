package me.moiclathy.dism.nl.query

/** A read-only semantic query compiled from natural language. */
data class SemanticStateQuery(
    val subjectEntityId: String? = null,
    val predicate: String? = null,
    val objectEntityId: String? = null,
    val queryKind: SemanticStateQueryKind = SemanticStateQueryKind.STATE,
    val confidence: Double = 0.0,
)

enum class SemanticStateQueryKind {
    STATE,
    RELATION,
    REQUIREMENTS,
    LOCATION,
}
