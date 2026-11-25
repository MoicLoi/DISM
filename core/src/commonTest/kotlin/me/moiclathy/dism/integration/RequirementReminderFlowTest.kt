package me.moiclathy.dism.integration

import me.moiclathy.dism.attention.AttentionEngine
import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.planning.recommendation.RecommendationEngine
import me.moiclathy.dism.planning.recommendation.RecommendationMessage
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.semantic.propagation.RequirementRiskEvaluator
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RequirementReminderFlowTest {

    private val riskEvaluator = RequirementRiskEvaluator()
    private val recommendationEngine = RecommendationEngine()
    private val attentionEngine = AttentionEngine()

    @Test
    fun upcomingEventWithMissingRequiredItem_shouldReachAttentionOutput() {
        val userId = Uuid.random()
        val eventId = Uuid.random()
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()

        val now = Instant.parse("2026-09-07T22:00:00Z")
        val eventTime = Instant.parse("2026-09-07T23:00:00Z")

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
                confidence = 0.95,
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

        val risks = riskEvaluator.evaluate(
            eventId = eventId,
            states = states,
            context = context,
            userId = userId,
        )


        val user = Entity(
            id = userId,
            type = EntityType.PERSON,
            name = "用户",
        )

        val event = Entity(
            id = eventId,
            type = EntityType.EVENT,
            name = "早操",
        )

        val powerBank = Entity(
            id = powerBankId,
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            id = dormId,
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val registry = EntityRegistry().apply {
            registerAll(
                listOf(
                    user,
                    event,
                    powerBank,
                    dorm,
                )
            )
        }
        assertEquals(1, risks.size)

        val recommendations =
            recommendationEngine.fromRequirementRisks(
                risks = risks,
                entities = registry,
            )

        assertEquals(1, recommendations.size)

        val selected = attentionEngine.select(
            recommendations = recommendations,
            policy = AttentionPolicy(
                softLimit = 5,
                hardLimit = 8,
                minimumPriority = 0.25,
            ),
        )

        assertEquals(1, selected.size)

        val recommendation = selected.single()

        assertEquals(
            RecommendationType.PREPARE_ITEM,
            recommendation.type,
        )

        assertTrue(
            recommendation.priority >= 0.25
        )

        val message = assertIs<
            RecommendationMessage.PrepareItemKnownLocation
            >(recommendation.message)

        assertEquals("早操", message.eventName)
        assertEquals("充电宝", message.itemName)
        assertEquals("宿舍", message.locationName)
    }
}
