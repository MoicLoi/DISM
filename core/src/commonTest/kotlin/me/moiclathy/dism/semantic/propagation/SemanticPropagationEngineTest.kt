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
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SemanticPropagationEngineTest {

    private val propagationEngine =
        SemanticPropagationEngine()

    @Test
    fun itemLocationChange_shouldTriggerRequirementRiskReevaluation() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()
        val userId = Uuid.random()
        val dormId = Uuid.random()

        val time =
            Instant.parse("2026-09-08T08:00:00Z")

        val requirement =
            RelationState(
                key = RelationKey(
                    subjectId = eventId,
                    predicate = Predicates.REQUIRES,
                    objectId = itemId,
                ),
                confidence = 1.0,
                supports = emptySet(),
                validFrom = time,
            )

        val locatedAtDorm =
            RelationState(
                key = RelationKey(
                    subjectId = itemId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 1.0,
                supports = emptySet(),
                validFrom = time,
            )

        val changeEvent =
            StateChangeEvent(
                transitionRecord =
                    StateTransitionRecord(
                        effectiveAt = time,
                        transition =
                            StateTransition.AssertRelation(
                                key =
                                    locatedAtDorm.key,
                                semanticConfidence =
                                    1.0,
                                effectiveAt =
                                    time,
                                evidenceIds =
                                    emptySet(),
                            ),
                    ),
                changes =
                    listOf(
                        StateChange.Inserted(
                            locatedAtDorm
                        )
                    ),
            )

        val result =
            propagationEngine.propagate(
                event = changeEvent,
                states =
                    listOf(
                        requirement,
                        locatedAtDorm,
                    ),
                context =
                    Context(
                        now = time,
                        upcomingEvents =
                            listOf(
                                ContextEvent(
                                    eventId =
                                        eventId,
                                    startAt =
                                        time + 1.hours,
                                )
                            ),
                    ),
                userId =
                    userId,
            )

        assertEquals(1, result.requirementRisks.size)

        assertEquals(
            itemId,
            result.requirementRisks
                .single()
                .requiredEntityId,
        )

        assertEquals(
            eventId,
            result.requirementRisks
                .single()
                .eventId,
        )
    }

    @Test
    fun unrelatedRelationChange_shouldNotTriggerReevaluation() {
        val eventId = Uuid.random()
        val entityId = Uuid.random()

        val time =
            Instant.parse("2026-09-08T08:00:00Z")

        val unrelatedRelation =
            RelationState(
                key = RelationKey(
                    subjectId = eventId,
                    predicate = Predicate("belongs_to"),
                    objectId = entityId,
                ),
                confidence = 1.0,
                supports = emptySet(),
                validFrom = time,
            )

        val changeEvent =
            StateChangeEvent(
                transitionRecord =
                    StateTransitionRecord(
                        effectiveAt = time,
                        transition =
                            StateTransition.AssertRelation(
                                key =
                                    unrelatedRelation.key,
                                semanticConfidence =
                                    1.0,
                                effectiveAt =
                                    time,
                                evidenceIds =
                                    emptySet(),
                            ),
                    ),
                changes =
                    listOf(
                        StateChange.Inserted(
                            unrelatedRelation
                        )
                    ),
            )

        val result =
            propagationEngine.propagate(
                event = changeEvent,
                states =
                    listOf(unrelatedRelation),
                context =
                    Context(now = time),
                userId = Uuid.random(),
            )

        // propagation trigger 不是"任何状态都重算"
        assertEquals(
            emptyList(),
            result.requirementRisks,
        )
    }
}
