package me.moiclathy.dism.nl.query.resolution

import me.moiclathy.dism.nl.query.SemanticStateQuery
import me.moiclathy.dism.nl.resolution.EntityResolutionResult

/** Resolved read-only query. Resolution never mutates entities or semantic state. */
data class SemanticStateQueryResolution(
    val status: SemanticStateQueryResolutionStatus,
    val query: SemanticStateQuery? = null,
    val subjectResolution: EntityResolutionResult? = null,
    val objectResolution: EntityResolutionResult? = null,
    val unresolvedMentions: List<String> = emptyList(),
    val confidence: Double = 0.0,
    val rationale: String = "",
)
