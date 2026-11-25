package me.moiclathy.dism.nl.query.e2e

/** Deterministic trace for the natural-language query path. */
data class SemanticQueryE2ETrace(
    val input: String,
    val compiled: Boolean,
    val resolved: Boolean,
    val executable: Boolean,
    val answered: Boolean,
    val response: String? = null,
)
