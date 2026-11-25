package me.moiclathy.dism.semantic.resolution

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ResolutionContextBuilderTest {

    private val builder = ResolutionContextBuilder()

    @Test
    fun activeEventRequirement_shouldCreateStrongEntityHint() {
        val eventId = Uuid.random()
        val powerBankId = Uuid.random()

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val requirement = RelationState(
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
            validFrom = time,
        )

        val context = Context(
            now = time,
            activeEventIds = setOf(eventId),
        )

        val result = builder.build(
            states = listOf(requirement),
            context = context,
        )

        val hint = result.entityHints
            .single { it.entityId == powerBankId }

        assertEquals(1.0, hint.relevance)
    }

    @Test
    fun upcomingEventRequirement_shouldCreateContextHint() {
        val eventId = Uuid.random()
        val campusCardId = Uuid.random()

        val now = Instant.parse("2026-09-08T08:00:00Z")
        val eventTime = Instant.parse("2026-09-08T10:00:00Z")

        val requirement = RelationState(
            key = RelationKey(
                subjectId = eventId,
                predicate = Predicates.REQUIRES,
                objectId = campusCardId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = now,
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

        val result = builder.build(
            states = listOf(requirement),
            context = context,
        )

        val hint = result.entityHints.single()

        assertEquals(campusCardId, hint.entityId)
        assertEquals(0.85, hint.relevance)
    }

    @Test
    fun unrelatedRequirement_shouldNotCreateHint() {
        val unrelatedEventId = Uuid.random()
        val powerBankId = Uuid.random()

        val now = Instant.parse("2026-09-08T08:00:00Z")

        val requirement = RelationState(
            key = RelationKey(
                subjectId = unrelatedEventId,
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
        )

        val context = Context(now = now)

        val result = builder.build(
            states = listOf(requirement),
            context = context,
        )

        assertEquals(emptyList(), result.entityHints)
    }

    @Test
    fun requirementConfidence_shouldScaleHintRelevance() {
        val eventId = Uuid.random()
        val powerBankId = Uuid.random()

        val now = Instant.parse("2026-09-08T08:00:00Z")

        val requirement = RelationState(
            key = RelationKey(
                subjectId = eventId,
                predicate = Predicates.REQUIRES,
                objectId = powerBankId,
            ),
            confidence = 0.4,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 0.4,
                )
            ),
            validFrom = now,
        )

        val context = Context(
            now = now,
            activeEventIds = setOf(eventId),
        )

        val result = builder.build(
            states = listOf(requirement),
            context = context,
        )

        val hint = result.entityHints.single()

        assertEquals(0.4, hint.relevance)
    }

    @Test
    fun multipleEventsRequiringSameEntity_shouldKeepStrongestHint() {
        val activeEventId = Uuid.random()
        val upcomingEventId = Uuid.random()
        val powerBankId = Uuid.random()

        val now = Instant.parse("2026-09-08T08:00:00Z")
        val eventTime = Instant.parse("2026-09-08T10:00:00Z")

        val states = listOf(
            RelationState(
                key = RelationKey(
                    subjectId = activeEventId,
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
                    subjectId = upcomingEventId,
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
        )

        val context = Context(
            now = now,
            activeEventIds = setOf(activeEventId),
            upcomingEvents = listOf(
                ContextEvent(
                    eventId = upcomingEventId,
                    startAt = eventTime,
                ),
            ),
        )

        val result = builder.build(
            states = states,
            context = context,
        )

        val hint = result.entityHints.single()

        assertEquals(powerBankId, hint.entityId)
        assertEquals(1.0, hint.relevance)
    }

    @Test
    fun recentEntityReference_shouldCreateStrongHint() {
        val entityId = Uuid.random()

        val now = Instant.parse("2026-09-08T08:05:00Z")

        val memory = ResolutionMemory()
        memory.record(
            entityId = entityId,
            referencedAt = Instant.parse("2026-09-08T08:04:30Z"),
            confidence = 1.0,
        )

        val result = builder.build(
            states = emptyList(),
            context = Context(now = now),
            memory = memory,
        )

        val hint = result.entityHints.single()

        assertEquals(entityId, hint.entityId)
        assertEquals(1.0, hint.relevance)
    }

    @Test
    fun oldEntityReference_shouldNotCreateHint() {
        val entityId = Uuid.random()

        val memory = ResolutionMemory()
        memory.record(
            entityId = entityId,
            referencedAt = Instant.parse("2026-09-08T06:00:00Z"),
        )

        val result = builder.build(
            states = emptyList(),
            context = Context(
                now = Instant.parse("2026-09-08T08:00:00Z")
            ),
            memory = memory,
        )

        assertEquals(emptyList(), result.entityHints)
    }
}
