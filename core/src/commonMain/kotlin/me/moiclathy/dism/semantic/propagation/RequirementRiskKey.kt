package me.moiclathy.dism.semantic.propagation

import kotlin.uuid.Uuid

data class RequirementRiskKey(
    val eventId: Uuid,
    val requiredEntityId: Uuid,
)

fun RequirementRisk.key(): RequirementRiskKey =
    RequirementRiskKey(
        eventId = eventId,
        requiredEntityId = requiredEntityId,
    )
