package me.moiclathy.dism.planning.recommendation

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.semantic.propagation.RequirementRisk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class RecommendationEngineTest {

    private val engine = RecommendationEngine()

    @Test
    fun requirementRisk_withoutLocation_shouldProduceUnknownLocationMessage() {
        val event = Entity(
            type = EntityType.EVENT,
            name = "早操",
        )

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val registry = EntityRegistry().apply {
            register(event)
            register(powerBank)
        }

        val risk = RequirementRisk(
            eventId = event.id,
            requiredEntityId = powerBank.id,
            confidence = 0.9,
            severity = 0.8,
            reasons = listOf(
                "Event requires this entity",
            ),
        )

        val recommendations =
            engine.fromRequirementRisks(
                risks = listOf(risk),
                entities = registry,
            )

        val recommendation =
            recommendations.single()

        val message = assertIs<
            RecommendationMessage.PrepareItemUnknownLocation
            >(recommendation.message)

        assertEquals("早操", message.eventName)
        assertEquals("充电宝", message.itemName)
    }

    @Test
    fun requirementRisk_withLocation_shouldProduceKnownLocationMessage() {
        val event = Entity(
            type = EntityType.EVENT,
            name = "早操",
        )

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val registry = EntityRegistry().apply {
            register(event)
            register(powerBank)
            register(dorm)
        }

        val risk = RequirementRisk(
            eventId = event.id,
            requiredEntityId = powerBank.id,
            knownLocationId = dorm.id,
            confidence = 0.9,
            severity = 0.8,
            reasons = listOf(
                "Event requires this entity",
            ),
        )

        val recommendations =
            engine.fromRequirementRisks(
                risks = listOf(risk),
                entities = registry,
            )

        val recommendation =
            recommendations.single()

        val message = assertIs<
            RecommendationMessage.PrepareItemKnownLocation
            >(recommendation.message)

        assertEquals("早操", message.eventName)
        assertEquals("充电宝", message.itemName)
        assertEquals("宿舍", message.locationName)
    }
}
