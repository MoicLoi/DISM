package me.moiclathy.dism.nl.observation

import me.moiclathy.dism.nl.SemanticIntent
import me.moiclathy.dism.nl.relation.SemanticRelationCandidate
import me.moiclathy.dism.nl.temporal.TemporalResolution
import me.moiclathy.dism.domain.entity.Entity

interface SemanticObservationCompiler {
    fun compile(
        sourceText: String,
        intent: SemanticIntent,
        subject: Entity?,
        relation: SemanticRelationCandidate?,
        temporal: TemporalResolution?,
    ): SemanticObservationCompilation
}
