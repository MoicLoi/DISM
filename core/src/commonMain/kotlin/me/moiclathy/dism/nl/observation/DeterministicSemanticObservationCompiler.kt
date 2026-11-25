package me.moiclathy.dism.nl.observation

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.nl.SemanticIntent
import me.moiclathy.dism.nl.relation.SemanticRelationCandidate
import me.moiclathy.dism.nl.temporal.TemporalResolution

class DeterministicSemanticObservationCompiler(
    private val registry: EntityRegistry? = null,
) : SemanticObservationCompiler {
    override fun compile(
        sourceText: String,
        intent: SemanticIntent,
        subject: Entity?,
        relation: SemanticRelationCandidate?,
        temporal: TemporalResolution?,
    ): SemanticObservationCompilation {
        val relationConfidence = relation?.confidence ?: 1.0
        val temporalConfidence = temporal?.confidence ?: 1.0
        val intentConfidence = intent.confidence
        val confidence = minOf(intentConfidence, relationConfidence, temporalConfidence)
        val objectEntity = relation?.objectId?.let { registry?.find(it) }
        val unresolved = relation != null && (subject == null || objectEntity == null)
        return SemanticObservationCompilation(
            sourceText = sourceText,
            intent = intent,
            subject = subject,
            predicate = relation?.predicate?.let { Predicate(it.lowercase()) },
            `object` = objectEntity,
            temporal = temporal,
            confidence = confidence.coerceIn(0.0, 1.0),
            unresolved = unresolved,
        )
    }
}
