package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationState
import kotlin.uuid.Uuid

class RequirementRiskEvaluator {

    fun evaluate(
        eventId: Uuid,
        states: List<RelationState>,
        context: Context,
        userId: Uuid,
    ): List<RequirementRisk> {

        val activeStates = states.filter { it.isActive }

        val requiredRelations = activeStates.filter { state ->
            state.key.subjectId == eventId &&
                    state.key.predicate == Predicates.REQUIRES
        }

        return requiredRelations.mapNotNull { requirement ->

            val requiredEntityId = requirement.key.objectId

            val carriedByUser = activeStates.any { state ->
                state.key.subjectId == requiredEntityId &&
                        state.key.predicate == Predicates.CARRIED_BY &&
                        state.key.objectId == userId
            }

            if (carriedByUser) {
                return@mapNotNull null
            }

            val knownLocation = activeStates.firstOrNull { state ->
                state.key.subjectId == requiredEntityId &&
                        state.key.predicate == Predicates.LOCATED_AT
            }

            val eventIsUpcoming = context.upcomingEvents.any {
                it.eventId == eventId
            }

            if (!eventIsUpcoming) {
                return@mapNotNull null
            }

            val reasons = buildList {
                add("Event requires this entity")

                if (knownLocation != null) {
                    add("Required entity has a known location")
                }

                add("Required entity is not currently carried by user")
            }

            val confidence = minOf(
                requirement.confidence,
                knownLocation?.confidence ?: 0.6,
            )

            RequirementRisk(
                eventId = eventId,
                requiredEntityId = requiredEntityId,
                knownLocationId = knownLocation?.key?.objectId,
                confidence = confidence,
                severity = 0.8,
                reasons = reasons,
            )
        }
    }
}
