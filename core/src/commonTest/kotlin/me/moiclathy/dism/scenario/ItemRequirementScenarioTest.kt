package me.moiclathy.dism.scenario

import me.moiclathy.dism.attention.AttentionPolicy
import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.persistence.repository.InMemoryRelationStateRepository
import me.moiclathy.dism.persistence.repository.InMemoryStateCommitRepository
import me.moiclathy.dism.persistence.repository.InMemoryTransitionLogRepository
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.planning.recommendation.RecommendationMessage
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.runtime.DismRuntime
import me.moiclathy.dism.runtime.DismRuntimeApi
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/**
 * 第一个真正的端到端场景测试。
 *
 * 不直接调用 RequirementRiskEvaluator / RecommendationEngine / AttentionEngine，
 * 只像 App 一样对 DismRuntimeApi 说话：
 *
 *   现实："充电宝在宿舍"
 *   现实："明早实验课需要充电宝"
 *          ↓
 *   DISM："需要准备充电宝（在宿舍）"  ← 可见提醒
 *          ↓
 *   现实："我把充电宝带上了"
 *          ↓
 *   风险/提醒自动消失
 *
 * 全程使用内存持久化（commonTest 无 JDBC），
 * 持久化恢复场景见 jvmTest 的 ItemRequirementPersistenceScenarioTest。
 */
class ItemRequirementScenarioTest {

    private class World {

        val user =
            Entity(type = EntityType.PERSON, name = "我")
        val powerBank =
            Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm =
            Entity(type = EntityType.LOCATION, name = "宿舍")
        val experiment =
            Entity(type = EntityType.EVENT, name = "实验课")

        private val entities = EntityRegistry()
        private val evidence = InMemoryEvidenceRepository()
        private val relationStates = InMemoryRelationStateRepository()
        private val transitionLog = InMemoryTransitionLogRepository()

        val api: DismRuntimeApi =
            DismRuntime(
                DismPersistence(
                    entities = entities,
                    evidence = evidence,
                    relationStates = relationStates,
                    transitionLog = transitionLog,
                    stateCommit =
                        InMemoryStateCommitRepository(
                            relationStates,
                            transitionLog,
                        ),
                )
            )

        init {
            entities.saveAll(
                listOf(user, powerBank, dorm, experiment)
            )
        }

        fun upcomingContext(
            now: Instant,
        ): Context =
            Context(
                now = now,
                upcomingEvents =
                    listOf(
                        ContextEvent(
                            eventId = experiment.id,
                            startAt = now + 12.hours,
                        )
                    ),
            )

        /**
         * 以一句"现实陈述"驱动世界：登记证据 → observe（纯状态）。
         */
        fun state(
            subject: String,
            subjectType: EntityType,
            predicate: Predicate,
            objectText: String,
            objectType: EntityType,
            at: Instant,
        ) {
            val evidence =
                Evidence(
                    type = EvidenceType.EXPLICIT_STATEMENT,
                    source = "user",
                    observedAt = at,
                    confidence = 1.0,
                )
            this.evidence.save(evidence)

            val result =
                api.observe(
                    draft =
                        SemanticObservationDraft(
                            subjectMention =
                                EntityMention(subject, subjectType),
                            predicate = predicate,
                            objectMention =
                                EntityMention(objectText, objectType),
                            confidence = 1.0,
                            evidenceIds = setOf(evidence.id),
                            observedAt = at,
                        ),
                    context = Context(now = at),
                )

            assertIs<ObserveResult.Applied>(result)
        }

        /**
         * 录入一句现实并立即跑完整反应链（状态 → 风险 → 建议 → 注意力）。
         */
        fun evaluate(
            subject: String,
            subjectType: EntityType,
            predicate: Predicate,
            objectText: String,
            objectType: EntityType,
            at: Instant,
        ) = api.observeAndEvaluate(
            draft =
                SemanticObservationDraft(
                    subjectMention =
                        EntityMention(subject, subjectType),
                    predicate = predicate,
                    objectMention =
                        EntityMention(objectText, objectType),
                    confidence = 1.0,
                    evidenceIds = setOf(newEvidence(at).id),
                    observedAt = at,
                ),
            context = upcomingContext(at),
            userId = user.id,
            attentionPolicy = AttentionPolicy(),
        )

        private fun newEvidence(
            at: Instant,
        ): Evidence {
            val evidence =
                Evidence(
                    type = EvidenceType.EXPLICIT_STATEMENT,
                    source = "user",
                    observedAt = at,
                    confidence = 1.0,
                )
            this.evidence.save(evidence)
            return evidence
        }
    }

    @Test
    fun itemLeftInDorm_shouldRaiseVisiblePrepareItemThenClearWhenCarried() {

        val world = World()

        val evening =
            Instant.parse("2026-09-10T20:00:00Z")
        val nextMorning =
            Instant.parse("2026-09-11T07:30:00Z")

        // 现实："明早实验课需要充电宝"
        world.state(
            subject = "实验课",
            subjectType = EntityType.EVENT,
            predicate = Predicates.REQUIRES,
            objectText = "充电宝",
            objectType = EntityType.ITEM,
            at = evening,
        )

        // 现实："充电宝在宿舍" —— 用户不在宿舍 → 应产生可见提醒
        val firstResult =
            world.evaluate(
                subject = "充电宝",
                subjectType = EntityType.ITEM,
                predicate = Predicates.LOCATED_AT,
                objectText = "宿舍",
                objectType = EntityType.LOCATION,
                at = nextMorning,
            )

        assertIs<ObserveResult.Applied>(
            firstResult.observeResult
        )

        // 传播确实产生了风险（不只是 Recommendation 层有东西）
        assertTrue(
            firstResult.propagationResults
                .flatMap { it.requirementRisks }
                .isNotEmpty(),
            "充电宝在宿舍且明早实验课需要它，应产生 RequirementRisk",
        )

        assertTrue(
            firstResult.visibleRecommendations.isNotEmpty(),
            "应显示至少一条提醒",
        )

        val recommendation =
            firstResult.visibleRecommendations.single()

        assertEquals(
            RecommendationType.PREPARE_ITEM,
            recommendation.type,
        )

        val message =
            assertIs<RecommendationMessage.PrepareItemKnownLocation>(
                recommendation.message,
            )
        assertEquals("实验课", message.eventName)
        assertEquals("充电宝", message.itemName)
        assertEquals("宿舍", message.locationName)

        // 现实变化："我把充电宝带上了"
        val carryTime =
            Instant.parse("2026-09-11T07:40:00Z")

        val secondResult =
            world.evaluate(
                subject = "充电宝",
                subjectType = EntityType.ITEM,
                predicate = Predicates.CARRIED_BY,
                objectText = "我",
                objectType = EntityType.PERSON,
                at = carryTime,
            )

        assertIs<ObserveResult.Applied>(
            secondResult.observeResult
        )

        // 风险是当前世界状态的派生结果——带上后应消失
        assertTrue(
            secondResult.propagationResults
                .flatMap { it.requirementRisks }
                .none { it.requiredEntityId == world.powerBank.id },
            "充电宝被随身携带后，针对它的 RequirementRisk 应消失",
        )

        assertTrue(
            secondResult.visibleRecommendations
                .none { it.type == RecommendationType.PREPARE_ITEM },
            "不应再显示 PREPARE_ITEM 提醒",
        )

        // 状态层面：carried_by 已成为活动关系
        assertTrue(
            world.api.currentStates().any { state ->
                state.key.predicate == Predicates.CARRIED_BY &&
                    state.key.subjectId == world.powerBank.id &&
                    state.key.objectId == world.user.id
            },
            "充电宝应处于 carried_by 我 的活动状态",
        )
    }
}
