package me.moiclathy.dism.semantic.propagation

import kotlin.uuid.Uuid

sealed interface SemanticFact {

    data class RequirementRisk(
        val eventId: Uuid,
        val requiredEntityId: Uuid,
        val confidence: Double,
        val severity: Double,
    ) : SemanticFact {
        init {
            require(confidence in 0.0..1.0)
            require(severity in 0.0..1.0)
        }
    }
}