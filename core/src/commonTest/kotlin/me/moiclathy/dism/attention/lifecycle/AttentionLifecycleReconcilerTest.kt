package me.moiclathy.dism.attention.lifecycle

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.planning.recommendation.RecommendationEngine
import me.moiclathy.dism.planning.recommendation.RecommendationLifecycle
import me.moiclathy.dism.planning.recommendation.RecommendationLifecycleChange
import me.moiclathy.dism.planning.recommendation.RecommendationMessage
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.semantic.propagation.RequirementRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant

class AttentionLifecycleReconcilerTest {

    private val now = Instant.parse("2026-01-01T08:00:00Z")

    @Test
    fun createdRecommendation_shouldShowOnce() {
        val recommendation = recommendation()
        val key = requireNotNull(recommendation.key)
        val change = RecommendationLifecycleChange(
            key = key,
            lifecycle = RecommendationLifecycle.CREATED,
            current = recommendation,
        )

        val decision = AttentionLifecycleReconciler().reconcile(
            recommendationChanges = listOf(change),
            now = now,
        ).single()

        assertEquals(AttentionAction.SHOW, decision.action)
        assertEquals(1, decision.record.shownCount)
    }

    @Test
    fun sameRecommendation_insideCooldown_shouldBeSuppressed() {
        val recommendation = recommendation()
        val reconciler = AttentionLifecycleReconciler(30.minutes)
        val first = reconciler.reconcile(
            recommendationChanges = listOf(
                RecommendationLifecycleChange(
                    key = requireNotNull(recommendation.key),
                    lifecycle = RecommendationLifecycle.CREATED,
                    current = recommendation,
                )
            ),
            now = now,
        ).single()

        val second = reconciler.evaluateExisting(
            recommendation = recommendation.copy(id = kotlin.uuid.Uuid.random()),
            record = first.record,
            now = now + 10.minutes,
        )

        assertEquals(AttentionAction.SUPPRESS, second.action)
        assertEquals(1, second.record.shownCount)
    }

    @Test
    fun sameRecommendation_afterCooldown_shouldRepeat() {
        val recommendation = recommendation()
        val reconciler = AttentionLifecycleReconciler(30.minutes)
        val first = reconciler.reconcile(
            listOf(
                RecommendationLifecycleChange(
                    key = requireNotNull(recommendation.key),
                    lifecycle = RecommendationLifecycle.CREATED,
                    current = recommendation,
                )
            ),
            now = now,
        ).single()

        val second = reconciler.evaluateExisting(
            recommendation = recommendation.copy(id = kotlin.uuid.Uuid.random()),
            record = first.record,
            now = now + 31.minutes,
        )

        assertEquals(AttentionAction.REPEAT, second.action)
        assertEquals(2, second.record.shownCount)
    }

    @Test
    fun resolvedRecommendation_shouldBecomeTerminal() {
        val recommendation = recommendation()
        val key = requireNotNull(recommendation.key)
        val previous = AttentionRecord(
            key = AttentionKey(key),
            lifecycle = AttentionLifecycle.SHOWN,
            firstSeenAt = now,
            lastShownAt = now,
            shownCount = 1,
        )

        val decision = AttentionLifecycleReconciler().reconcile(
            recommendationChanges = listOf(
                RecommendationLifecycleChange(
                    key = key,
                    lifecycle = RecommendationLifecycle.RESOLVED,
                    previous = recommendation,
                )
            ),
            previousRecords = mapOf(previous.key to previous),
            now = now,
        ).single()

        assertEquals(AttentionAction.RESOLVED, decision.action)
        assertEquals(AttentionLifecycle.RESOLVED, decision.record.lifecycle)
    }

    private fun recommendation(): Recommendation {
        val event = Entity(type = EntityType.EVENT, name = "实验课")
        val item = Entity(type = EntityType.ITEM, name = "实验服")
        val registry = EntityRegistry().apply {
            register(event)
            register(item)
        }
        val risk = RequirementRisk(
            eventId = event.id,
            requiredEntityId = item.id,
            confidence = 0.9,
            severity = 0.8,
            reasons = listOf("required"),
        )
        return RecommendationEngine().fromRequirementRisks(listOf(risk), registry).single()
    }
}
