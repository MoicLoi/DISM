package me.moiclathy.dism.nl.query.execution

import me.moiclathy.dism.nl.query.SemanticStateQuery

/** A resolved query handed to a later state-query engine. */
data class SemanticStateQueryExecution(
    val query: SemanticStateQuery,
    val executable: Boolean,
    val reason: String = "",
)
