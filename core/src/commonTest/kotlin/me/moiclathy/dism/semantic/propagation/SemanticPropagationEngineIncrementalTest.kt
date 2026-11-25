package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.temporal.StateChange
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.StateTransitionRecord
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SemanticPropagationEngineIncrementalTest {

    private val engine = SemanticPropagationEngine()
    private val time = Instant.parse("2026-09-10T08:00:00Z")

    private fun state(
        subject: Uuid,
        predicate: Predicate,
        objectId: Uuid,
    ) = RelationState(
        key = RelationKey(subject, predicate, objectId),
        confidence = 1.0,
        supports = emptySet(),
        validFrom = time,
    )

    private fun changeEvent(state: RelationState) = StateChangeEvent(
        transitionRecord = StateTransitionRecord(
            effectiveAt = time,
            transition = StateTransition.AssertRelation(
                key = state.key,
                semanticConfidence = 1.0,
                effectiveAt = time,
                evidenceIds = emptySet(),
            ),
        ),
        changes = listOf(StateChange.Inserted(state)),
    )

    @Test
    fun itemChange_shouldOnlyReevaluateEventsRequiringThatItem() {
        val itemA = Uuid.random()
        val itemB = Uuid.random()
        val eventA = Uuid.random()
        val eventB = Uuid.random()
        val location = Uuid.random()
        val user = Uuid.random()

        val requirements = listOf(
            state(eventA, Predicates.REQUIRES, itemA),
            state(eventB, Predicates.REQUIRES, itemB),
        )

        val itemAState = state(itemA, Predicates.LOCATED_AT, location)
        val states = requirements + itemAState

        val result = engine.propagate(
            event = changeEvent(itemAState),
            states = states,
            context = Context(
                now = time,
                upcomingEvents = listOf(
                    ContextEvent(eventA, time),
                    ContextEvent(eventB, time),
                ),
            ),
            userId = user,
        )

        assertEquals(setOf(eventA), result.affectedEventIds)
        assertEquals(1, result.requirementRisks.size)
        assertEquals(eventA, result.requirementRisks.single().eventId)
    }

    @Test
    fun unrelatedRelation_shouldNotAffectAnyRequirement() {
        val event = Uuid.random()
        val item = Uuid.random()
        val unrelated = Uuid.random()

        val requirement = state(event, Predicates.REQUIRES, item)
        val unrelatedState = state(event, Predicate("belongs_to"), unrelated)

        val result = engine.propagate(
            event = changeEvent(unrelatedState),
            states = listOf(requirement, unrelatedState),
            context = Context(
                now = time,
                upcomingEvents = listOf(ContextEvent(event, time)),
            ),
            userId = Uuid.random(),
        )

        assertTrue(result.affectedEventIds.isEmpty())
        assertTrue(result.requirementRisks.isEmpty())
    }

    @Test
    fun requirementChange_shouldOnlyReevaluateItsOwnEvent() {
        val eventA = Uuid.random()
        val eventB = Uuid.random()
        val itemA = Uuid.random()
        val itemB = Uuid.random()

        val changed = state(eventA, Predicates.REQUIRES, itemA)
        val other = state(eventB, Predicates.REQUIRES, itemB)

        val result = engine.propagate(
            event = changeEvent(changed),
            states = listOf(changed, other),
            context = Context(
                now = time,
                upcomingEvents = listOf(
                    ContextEvent(eventA, time),
                    ContextEvent(eventB, time),
                ),
            ),
            userId = Uuid.random(),
        )

        assertEquals(setOf(eventA), result.affectedEventIds)
    }
}
