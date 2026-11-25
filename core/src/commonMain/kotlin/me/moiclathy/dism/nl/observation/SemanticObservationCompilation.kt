package me.moiclathy.dism.nl.observation

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.nl.SemanticIntent
import me.moiclathy.dism.nl.relation.SemanticRelationCandidate
import me.moiclathy.dism.nl.temporal.TemporalResolution

/** Candidate produced by the NL layer; it does not mutate domain state. */
data class SemanticObservationCompilation(
    val sourceText: String,
    val intent: SemanticIntent,
    val subject: Entity? = null,
    val predicate: Predicate? = null,
    val `object`: Entity? = null,
    val temporal: TemporalResolution? = null,
    val confidence: Double,
    val unresolved: Boolean = false,
)
