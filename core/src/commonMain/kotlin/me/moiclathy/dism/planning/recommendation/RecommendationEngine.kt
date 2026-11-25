package me.moiclathy.dism.planning.recommendation

import me.moiclathy.dism.persistence.repository.EntityRepository
import me.moiclathy.dism.semantic.propagation.RequirementRisk

class RecommendationEngine {

    fun fromRequirementRisks(
        risks: List<RequirementRisk>,
        entities: EntityRepository,
    ): List<Recommendation> {

        return risks.map { risk ->

            val eventName =
                entities.find(risk.eventId)?.name
                    ?: "Unknown event"

            val requiredEntityName =
                entities.find(risk.requiredEntityId)?.name
                    ?: "Required item"

            val knownLocationName =
                risk.knownLocationId
                    ?.let(entities::find)
                    ?.name

            val priority = (
                    risk.severity * risk.confidence
                    ).coerceIn(0.0, 1.0)

            val message =
                if (knownLocationName != null) {
                    RecommendationMessage.PrepareItemKnownLocation(
                        eventName = eventName,
                        itemName = requiredEntityName,
                        locationName = knownLocationName,
                    )
                } else {
                    RecommendationMessage.PrepareItemUnknownLocation(
                        eventName = eventName,
                        itemName = requiredEntityName,
                    )
                }

            Recommendation(
                type = RecommendationType.PREPARE_ITEM,
                message = message,
                priority = priority,
                confidence = risk.confidence,
                reasons = risk.reasons,
                key = RecommendationKey(
                    type = RecommendationType.PREPARE_ITEM,
                    eventId = risk.eventId,
                    requiredEntityId = risk.requiredEntityId,
                ),
            )
        }
    }
}
