package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.temporal.StateChange
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SemanticPropagationRiskLifecycleTest {

    @Test
    fun sameRiskAcrossEvaluations_shouldBecomePersisting() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()
        val userId = Uuid.random()
        val locationId = Uuid.random()
        val time = Instant.parse("2026-09-08T08:00:00Z")

        val requires = RelationState(
            key = RelationKey(eventId, Predicates.REQUIRES, itemId),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = time,
        )
        val located = RelationState(
            key = RelationKey(itemId, Predicates.LOCATED_AT, locationId),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = time,
        )

        val change = StateChange.Inserted(located)
        val event = StateChangeEvent(
            transitionRecord = StateTransitionRecord(
                effectiveAt = time,
                transition = StateTransition.AssertRelation(
                    key = located.key,
                    semanticConfidence = 1.0,
                    effectiveAt = time,
                    evidenceIds = emptySet(),
                ),
            ),
            changes = listOf(change),
        )
        val context = Context(
            now = time,
            upcomingEvents = listOf(ContextEvent(eventId, time.plus(kotlin.time.Duration.parse("1h")))),
        )

        val engine = SemanticPropagationEngine()
        val first = engine.propagate(
            event = event,
            states = listOf(requires, located),
            context = context,
            userId = userId,
        )
        val second = engine.propagate(
            event = event,
            states = listOf(requires, located),
            context = context,
            userId = userId,
            previousRequirementRisks = first.requirementRisks,
        )

        assertEquals(RequirementRiskLifecycle.CREATED, first.requirementRiskLifecycleChanges.single().lifecycle)
        assertEquals(RequirementRiskLifecycle.PERSISTING, second.requirementRiskLifecycleChanges.single().lifecycle)
    }

    @Test
    fun resolvedRisk_shouldBeReportedWhenItemBecomesCarried() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()
        val userId = Uuid.random()
        val locationId = Uuid.random()
        val time = Instant.parse("2026-09-08T08:00:00Z")

        val requires = RelationState(
            key = RelationKey(eventId, Predicates.REQUIRES, itemId),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = time,
        )
        val located = RelationState(
            key = RelationKey(itemId, Predicates.LOCATED_AT, locationId),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = time,
        )
        val carried = RelationState(
            key = RelationKey(itemId, Predicates.CARRIED_BY, userId),
            confidence = 1.0,
            supports = emptySet(),
            validFrom = time,
        )

        val initialEvent = StateChangeEvent(
            transitionRecord = StateTransitionRecord(
                effectiveAt = time,
                transition = StateTransition.AssertRelation(
                    key = located.key,
                    semanticConfidence = 1.0,
                    effectiveAt = time,
                    evidenceIds = emptySet(),
                ),
            ),
            changes = listOf(StateChange.Inserted(located)),
        )
        val carriedEvent = StateChangeEvent(
            transitionRecord = StateTransitionRecord(
                effectiveAt = time,
                transition = StateTransition.AssertRelation(
                    key = carried.key,
                    semanticConfidence = 1.0,
                    effectiveAt = time,
                    evidenceIds = emptySet(),
                ),
            ),
            changes = listOf(StateChange.Inserted(carried)),
        )
        val context = Context(
            now = time,
            upcomingEvents = listOf(ContextEvent(eventId, time.plus(kotlin.time.Duration.parse("1h")))),
        )

        val engine = SemanticPropagationEngine()
        val first = engine.propagate(
            event = initialEvent,
            states = listOf(requires, located),
            context = context,
            userId = userId,
        )
        val resolved = engine.propagate(
            event = carriedEvent,
            states = listOf(requires, located, carried),
            context = context,
            userId = userId,
            previousRequirementRisks = first.requirementRisks,
        )

        assertEquals(emptyList(), resolved.requirementRisks)
        assertEquals(RequirementRiskLifecycle.RESOLVED, resolved.requirementRiskLifecycleChanges.single().lifecycle)
    }
}
