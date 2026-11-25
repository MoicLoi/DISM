package me.moiclathy.dism.planning.recommendation

import me.moiclathy.dism.semantic.propagation.RequirementRiskKey
import me.moiclathy.dism.semantic.propagation.RequirementRiskLifecycle
import me.moiclathy.dism.semantic.propagation.RequirementRiskLifecycleChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class RecommendationLifecycleReconcilerTest {

    private val reconciler = RecommendationLifecycleReconciler()

    private val riskKey = RequirementRiskKey(
        eventId = Uuid.random(),
        requiredEntityId = Uuid.random(),
    )

    private fun recommendation(
        priority: Double = 0.8,
        location: String = "宿舍",
    ): Recommendation = Recommendation(
        type = RecommendationType.PREPARE_ITEM,
        message = RecommendationMessage.PrepareItemKnownLocation(
            eventName = "实验课",
            itemName = "实验服",
            locationName = location,
        ),
        priority = priority,
        confidence = 0.9,
        reasons = listOf("Event requires this entity"),
        key = RecommendationKey(
            type = RecommendationType.PREPARE_ITEM,
            eventId = riskKey.eventId,
            requiredEntityId = riskKey.requiredEntityId,
        ),
    )

    @Test
    fun missingPrevious_shouldBeCreated() {
        val current = recommendation()

        val changes = reconciler.reconcile(
            previousRecommendations = emptyList(),
            currentRecommendations = listOf(current),
        )

        assertEquals(1, changes.size)
        assertEquals(RecommendationLifecycle.CREATED, changes.single().lifecycle)
        assertEquals(current, changes.single().current)
    }

    @Test
    fun sameSemanticRecommendation_shouldNotProduceLifecycleChange() {
        val previous = recommendation()
        val regenerated = previous.copy(id = Uuid.random())

        val changes = reconciler.reconcile(
            previousRecommendations = listOf(previous),
            currentRecommendations = listOf(regenerated),
        )

        assertTrue(changes.isEmpty())
    }

    @Test
    fun changedRecommendation_shouldBeUpdated() {
        val previous = recommendation(location = "宿舍")
        val current = recommendation(location = "背包")

        val changes = reconciler.reconcile(
            previousRecommendations = listOf(previous),
            currentRecommendations = listOf(current),
        )

        assertEquals(1, changes.size)
        assertEquals(RecommendationLifecycle.UPDATED, changes.single().lifecycle)
        assertEquals(previous, changes.single().previous)
        assertEquals(current, changes.single().current)
    }

    @Test
    fun resolvedRisk_shouldResolveRecommendation() {
        val previous = recommendation()

        val changes = reconciler.reconcile(
            previousRecommendations = listOf(previous),
            currentRecommendations = emptyList(),
            riskLifecycleChanges = listOf(
                RequirementRiskLifecycleChange(
                    key = riskKey,
                    lifecycle = RequirementRiskLifecycle.RESOLVED,
                    previous = null,
                    current = null,
                )
            ),
        )

        assertEquals(1, changes.size)
        assertEquals(RecommendationLifecycle.RESOLVED, changes.single().lifecycle)
        assertEquals(previous, changes.single().previous)
        assertEquals(null, changes.single().current)
    }

    @Test
    fun expiredRisk_shouldExpireRecommendation() {
        val previous = recommendation()

        val changes = reconciler.reconcile(
            previousRecommendations = listOf(previous),
            currentRecommendations = emptyList(),
            riskLifecycleChanges = listOf(
                RequirementRiskLifecycleChange(
                    key = riskKey,
                    lifecycle = RequirementRiskLifecycle.EXPIRED,
                    previous = null,
                    current = null,
                )
            ),
        )

        assertEquals(1, changes.size)
        assertEquals(RecommendationLifecycle.EXPIRED, changes.single().lifecycle)
    }
}
