package me.moiclathy.dism.nl.query.adapter

/** Structured result returned by a state source; no presentation text is produced here. */
data class SemanticStateQueryStateResult(
    val found: Boolean,
    val values: List<SemanticStateQueryValue> = emptyList(),
    val confidence: Double = 1.0,
)

data class SemanticStateQueryValue(
    val subjectId: String? = null,
    val predicate: String,
    val objectId: String? = null,
    val objectName: String? = null,
    val confidence: Double = 1.0,
)
