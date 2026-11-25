package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RequirementRiskLifecycleReconcilerTest {

    private val reconciler = RequirementRiskLifecycleReconciler()

    private val now = Instant.parse("2026-09-08T08:00:00Z")

    private fun risk(
        eventId: Uuid,
        itemId: Uuid,
    ) = RequirementRisk(
        eventId = eventId,
        requiredEntityId = itemId,
        confidence = 0.9,
        severity = 0.8,
        reasons = listOf("test"),
    )

    @Test
    fun newRisk_shouldBeCreated() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()

        val changes = reconciler.reconcile(
            previousRisks = emptyList(),
            currentRisks = listOf(risk(eventId, itemId)),
            context = Context(now = now),
        )

        assertEquals(1, changes.size)
        assertEquals(RequirementRiskLifecycle.CREATED, changes.single().lifecycle)
        assertEquals(null, changes.single().previous)
        assertEquals(itemId, changes.single().key.requiredEntityId)
    }

    @Test
    fun existingRisk_shouldBePersisting() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()
        val previous = risk(eventId, itemId)
        val current = risk(eventId, itemId).copy(confidence = 0.95)

        val changes = reconciler.reconcile(
            previousRisks = listOf(previous),
            currentRisks = listOf(current),
            context = Context(now = now),
        )

        assertEquals(RequirementRiskLifecycle.PERSISTING, changes.single().lifecycle)
        assertEquals(previous, changes.single().previous)
        assertEquals(current, changes.single().current)
    }

    @Test
    fun missingRisk_shouldBeResolvedWhenEventHasNotEnded() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()

        val changes = reconciler.reconcile(
            previousRisks = listOf(risk(eventId, itemId)),
            currentRisks = emptyList(),
            context = Context(
                now = now,
                upcomingEvents = listOf(
                    ContextEvent(
                        eventId = eventId,
                        startAt = now.plus(kotlin.time.Duration.parse("1h")),
                        endAt = now.plus(kotlin.time.Duration.parse("2h")),
                    )
                ),
            ),
        )

        assertEquals(RequirementRiskLifecycle.RESOLVED, changes.single().lifecycle)
    }

    @Test
    fun missingRisk_shouldBeExpiredWhenEventHasEnded() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()

        val changes = reconciler.reconcile(
            previousRisks = listOf(risk(eventId, itemId)),
            currentRisks = emptyList(),
            context = Context(
                now = now,
                upcomingEvents = listOf(
                    ContextEvent(
                        eventId = eventId,
                        startAt = now.minus(kotlin.time.Duration.parse("2h")),
                        endAt = now.minus(kotlin.time.Duration.parse("1h")),
                    )
                ),
            ),
        )

        assertEquals(RequirementRiskLifecycle.EXPIRED, changes.single().lifecycle)
    }

    @Test
    fun unrelatedPreviousRisk_shouldNotBeMarkedByAnAffectedEvent() {
        val eventId = Uuid.random()
        val itemId = Uuid.random()
        val unrelatedEventId = Uuid.random()
        val unrelatedItemId = Uuid.random()

        val previous = listOf(
            risk(eventId, itemId),
            risk(unrelatedEventId, unrelatedItemId),
        )

        val changes = reconciler.reconcile(
            previousRisks = previous.filter { it.eventId == eventId },
            currentRisks = emptyList(),
            context = Context(now = now),
        )

        assertEquals(1, changes.size)
        assertEquals(eventId, changes.single().key.eventId)
    }
}
