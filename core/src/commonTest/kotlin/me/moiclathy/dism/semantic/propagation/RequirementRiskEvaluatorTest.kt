package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RequirementRiskEvaluatorTest {

    private val evaluator = RequirementRiskEvaluator()

    @Test
    fun requiredItemNotCarried_shouldProduceRisk() {
        val userId = Uuid.random()
        val eventId = Uuid.random()
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()

        val now = Instant.parse("2026-09-07T23:00:00Z")
        val eventTime = Instant.parse("2026-09-08T00:00:00Z")

        val states = listOf(
            RelationState(
                key = RelationKey(
                    subjectId = eventId,
                    predicate = Predicates.REQUIRES,
                    objectId = powerBankId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = Uuid.random(),
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = now,
            ),

            RelationState(
                key = RelationKey(
                    subjectId = powerBankId,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dormId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = Uuid.random(),
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = now,
            ),
        )

        val context = Context(
            now = now,
            upcomingEvents = listOf(
                ContextEvent(
                    eventId = eventId,
                    startAt = eventTime,
                ),
            ),
        )

        val risks = evaluator.evaluate(
            eventId = eventId,
            states = states,
            context = context,
            userId = userId,
        )

        assertEquals(1, risks.size)

        val risk = risks.single()

        assertEquals(eventId, risk.eventId)
        assertEquals(powerBankId, risk.requiredEntityId)

        assertTrue(risk.confidence > 0.0)
        assertTrue(risk.severity > 0.0)

        assertEquals(
            dormId,
            risk.knownLocationId,
        )
    }


    @Test
    fun requiredItemAlreadyCarried_shouldNotProduceRisk() {
        val userId = Uuid.random()
        val eventId = Uuid.random()
        val powerBankId = Uuid.random()

        val now = Instant.parse("2026-09-07T23:00:00Z")

        val states = listOf(
            RelationState(
                key = RelationKey(
                    subjectId = eventId,
                    predicate = Predicates.REQUIRES,
                    objectId = powerBankId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = Uuid.random(),
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = now,
            ),

            RelationState(
                key = RelationKey(
                    subjectId = powerBankId,
                    predicate = Predicates.CARRIED_BY,
                    objectId = userId,
                ),
                confidence = 1.0,
                supports = setOf(
                    RelationSupport(
                        evidenceId = Uuid.random(),
                        semanticConfidence = 1.0,
                    )
                ),
                validFrom = now,
            ),
        )

        val context = Context(
            now = now,
            upcomingEvents = listOf(
                ContextEvent(
                    eventId = eventId,
                    startAt = Instant.parse("2026-09-08T00:00:00Z"),
                ),
            ),
        )

        val risks = evaluator.evaluate(
            eventId = eventId,
            states = states,
            context = context,
            userId = userId,
        )

        assertTrue(risks.isEmpty())
    }
}
