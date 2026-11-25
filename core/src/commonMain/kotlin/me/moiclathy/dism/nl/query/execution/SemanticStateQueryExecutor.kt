package me.moiclathy.dism.nl.query.execution

import me.moiclathy.dism.nl.query.SemanticStateQuery

/** Boundary for executing a resolved state query. This stage does not access persistence. */
fun interface SemanticStateQueryExecutor {
    fun prepare(query: SemanticStateQuery): SemanticStateQueryExecution
}
