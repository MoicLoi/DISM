package me.moiclathy.dism.semantic

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SemanticEvaluatorTest {

    private val evaluator = SemanticEvaluator()

    @Test
    fun upcomingEvent_shouldIncreaseUrgency() {
        val now = Instant.parse("2026-09-07T23:00:00Z")

        val state = RelationState(
            key = RelationKey(
                subjectId = Uuid.random(),
                predicate = Predicates.LOCATED_AT,
                objectId = Uuid.random(),
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = Instant.parse("2026-09-07T20:00:00Z"),
        )

        val nearContext = Context(
            now = now,
            upcomingEvents = listOf(
                ContextEvent(
                    eventId = Uuid.random(),
                    startAt = Instant.parse("2026-09-08T00:00:00Z"),
                ),
            ),
        )

        val farContext = Context(
            now = now,
            upcomingEvents = listOf(
                ContextEvent(
                    eventId = Uuid.random(),
                    startAt = Instant.parse("2026-09-08T10:00:00Z"),
                ),
            ),
        )

        val nearEvaluation = evaluator.evaluate(
            state = state,
            context = nearContext,
        )

        val farEvaluation = evaluator.evaluate(
            state = state,
            context = farContext,
        )

        assertTrue(
            nearEvaluation.urgency > farEvaluation.urgency
        )

        assertTrue(
            nearEvaluation.weight > farEvaluation.weight
        )
    }
}
