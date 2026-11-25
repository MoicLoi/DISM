package me.moiclathy.dism.nl.query.adapter

import me.moiclathy.dism.nl.query.SemanticStateQuery

/** Read-only boundary for the existing semantic state. */
fun interface SemanticStateQueryStateSource {
    fun query(query: SemanticStateQuery): SemanticStateQueryStateResult
}
