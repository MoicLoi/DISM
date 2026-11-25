package me.moiclathy.dism.nl.query.adapter

import me.moiclathy.dism.nl.query.SemanticStateQuery

/** Executes only an already-resolved query against a supplied read-only state source. */
class SemanticStateQueryAdapter(
    private val source: SemanticStateQueryStateSource,
) {
    fun execute(query: SemanticStateQuery): SemanticStateQueryStateResult = source.query(query)
}
