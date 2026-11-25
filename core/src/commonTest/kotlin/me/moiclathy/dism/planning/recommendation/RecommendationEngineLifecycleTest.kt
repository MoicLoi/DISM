package me.moiclathy.dism.planning.recommendation

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.semantic.propagation.RequirementRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class RecommendationEngineLifecycleTest {

    @Test
    fun requirementRisk_shouldProduceStableRecommendationKey() {
        val event = Entity(
            type = EntityType.EVENT,
            name = "实验课",
        )
        val item = Entity(
            type = EntityType.ITEM,
            name = "实验服",
        )

        val registry = EntityRegistry().apply {
            register(event)
            register(item)
        }

        val risk = RequirementRisk(
            eventId = event.id,
            requiredEntityId = item.id,
            confidence = 0.9,
            severity = 0.8,
            reasons = listOf("Event requires this entity"),
        )

        val recommendation = RecommendationEngine()
            .fromRequirementRisks(listOf(risk), registry)
            .single()

        val key = assertNotNull(recommendation.key)
        assertEquals(RecommendationType.PREPARE_ITEM, key.type)
        assertEquals(event.id, key.eventId)
        assertEquals(item.id, key.requiredEntityId)
    }
}
