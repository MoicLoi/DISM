package me.moiclathy.dism.semantic.engine

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.planning.recommendation.RecommendationMessage
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * 整个 DISM Core MVP 最重要的一条链路：
 *
 * 用户只说发生了什么
 *     ↓
 * 系统维护世界状态
 *     ↓
 * 状态变化自动传播
 *     ↓
 * 产生少而准确的提醒
 */
class ReactiveEvaluationPipelineTest {

    private class World {
        val engine = SemanticStateEngine()

        val event = Entity(
            type = EntityType.EVENT,
            name = "早操",
        )

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        init {
            engine.entities.saveAll(
                listOf(event, powerBank, user, dorm)
            )
        }

        fun observe(
            subject: String,
            subjectType: EntityType,
            predicate: me.moiclathy.dism.domain.relation.Predicate,
            `object`: String,
            objectType: EntityType,
            time: Instant,
        ) {
            val evidence = Evidence(
                type = EvidenceType.EXPLICIT_STATEMENT,
                source = "user",
                observedAt = time,
                confidence = 1.0,
            )

            engine.evidence.save(evidence)

            engine.observe(
                draft = SemanticObservationDraft(
                    subjectMention = EntityMention(
                        text = subject,
                        expectedType = subjectType,
                    ),
                    predicate = predicate,
                    objectMention = EntityMention(
                        text = `object`,
                        expectedType = objectType,
                    ),
                    confidence = 1.0,
                    evidenceIds = setOf(evidence.id),
                    observedAt = time,
                ),
                context = Context(now = time),
            )
        }
    }

    @Test
    fun itemLeftBehind_shouldProduceVisiblePrepareItemRecommendation() {
        val world = World()

        val t0 = Instant.parse("2026-09-08T06:30:00Z")
        val t1 = Instant.parse("2026-09-08T07:00:00Z")
        val t2 = Instant.parse("2026-09-08T07:10:00Z")

        // 早操 requires 充电宝
        world.observe(
            subject = "早操",
            subjectType = EntityType.EVENT,
            predicate = Predicates.REQUIRES,
            `object` = "充电宝",
            objectType = EntityType.ITEM,
            time = t0,
        )

        // 充电宝我带上了
        world.observe(
            subject = "充电宝",
            subjectType = EntityType.ITEM,
            predicate = Predicates.CARRIED_BY,
            `object` = "我",
            objectType = EntityType.PERSON,
            time = t1,
        )

        // 用户："充电宝放宿舍了"
        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = t2,
            confidence = 1.0,
        )

        world.engine.evidence.save(evidence)

        val context = Context(
            now = t2,
            upcomingEvents = listOf(
                ContextEvent(
                    eventId = world.event.id,
                    startAt = t2 + 1.hours,
                )
            ),
        )

        val result = world.engine.observeAndEvaluate(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention(
                    text = "宿舍",
                    expectedType = EntityType.LOCATION,
                ),
                confidence = 1.0,
                evidenceIds = setOf(evidence.id),
                observedAt = t2,
            ),
            context = context,
            userId = world.user.id,
        )

        assertIs<ObserveResult.Applied>(result.observeResult)

        // carried_by 被 supersede，located_at Dorm 生效
        assertEquals(1, result.propagationResults.size)
        assertEquals(1, result.propagationResults.single().requirementRisks.size)

        assertEquals(1, result.visibleRecommendations.size)

        val recommendation = result.visibleRecommendations.single()

        assertEquals(
            RecommendationType.PREPARE_ITEM,
            recommendation.type,
        )

        val message = assertIs<
            RecommendationMessage.PrepareItemKnownLocation
            >(recommendation.message)

        assertEquals("早操", message.eventName)
        assertEquals("充电宝", message.itemName)
        assertEquals("宿舍", message.locationName)
    }

    @Test
    fun itemCarriedAgain_shouldClearVisibleRecommendation() {
        val world = World()

        val t0 = Instant.parse("2026-09-08T06:30:00Z")
        val t1 = Instant.parse("2026-09-08T07:00:00Z")
        val t2 = Instant.parse("2026-09-08T07:10:00Z")
        val t3 = Instant.parse("2026-09-08T07:20:00Z")

        // 早操 requires 充电宝
        world.observe(
            subject = "早操",
            subjectType = EntityType.EVENT,
            predicate = Predicates.REQUIRES,
            `object` = "充电宝",
            objectType = EntityType.ITEM,
            time = t0,
        )

        // 充电宝在宿舍 → 出现提醒
        world.observe(
            subject = "充电宝",
            subjectType = EntityType.ITEM,
            predicate = Predicates.LOCATED_AT,
            `object` = "宿舍",
            objectType = EntityType.LOCATION,
            time = t1,
        )

        val context = Context(
            now = t1,
            upcomingEvents = listOf(
                ContextEvent(
                    eventId = world.event.id,
                    startAt = t1 + 1.hours,
                )
            ),
        )

        // 带上以后，风险应消失
        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = t3,
            confidence = 1.0,
        )

        world.engine.evidence.save(evidence)

        val result = world.engine.observeAndEvaluate(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.CARRIED_BY,
                objectMention = EntityMention(
                    text = "我",
                    expectedType = EntityType.PERSON,
                ),
                confidence = 1.0,
                evidenceIds = setOf(evidence.id),
                observedAt = t3,
            ),
            context = Context(
                now = t3,
                upcomingEvents = listOf(
                    ContextEvent(
                        eventId = world.event.id,
                        startAt = t3 + 1.hours,
                    )
                ),
            ),
            userId = world.user.id,
        )

        // Recommendation 不是静态创建后永远存在，
        // 而是当前世界状态的派生结果。
        assertEquals(
            emptyList(),
            result.visibleRecommendations,
        )
    }
}
