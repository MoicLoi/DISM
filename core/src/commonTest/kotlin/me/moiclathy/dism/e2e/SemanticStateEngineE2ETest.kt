package me.moiclathy.dism.e2e

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.context.ContextEvent
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.planning.recommendation.RecommendationType
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.engine.SemanticStateEngine
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import kotlin.test.Test
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * 第一个端到端语义闭环（直接基于 SemanticStateEngine 的最纯基线）。
 *
 *   语义输入 → Observation → Entity Resolution → StateTransition
 *   → Dynamic State → Semantic Propagation
 *   → RequirementRisk → Recommendation → Attention
 *
 * 与 scenario 包测试的区别：
 * 这里不引入 Persistence / DismRuntimeApi Facade，
 * 只用 Core 的语义入口，证明"状态变化能自动改变系统对未来事件的判断"。
 *
 * 持久化重启闭环见 jvmTest 的 ItemRequirementPersistenceScenarioTest；
 * Facade 版闭环见 scenario.ItemRequirementScenarioTest。
 */
class SemanticStateEngineE2ETest {

    private val engine = SemanticStateEngine()

    private val user =
        Entity(type = EntityType.PERSON, name = "我")
    private val powerBank =
        Entity(type = EntityType.ITEM, name = "充电宝")
    private val dorm =
        Entity(type = EntityType.LOCATION, name = "宿舍")
    private val experiment =
        Entity(type = EntityType.EVENT, name = "实验课")

    init {
        engine.entities.saveAll(
            listOf(user, powerBank, dorm, experiment)
        )
    }

    /** 只登记一个"现实陈述"为世界状态（纯 observe，不做反应链）。 */
    private fun state(
        subject: String,
        subjectType: EntityType,
        predicate: Predicate,
        objectText: String,
        objectType: EntityType,
        at: Instant,
    ) {
        val evidence = newEvidence(at)
        val result =
            engine.observe(
                draft = draft(
                    subject, subjectType, predicate,
                    objectText, objectType, evidence.id, at,
                ),
                context = Context(now = at),
            )
        assertIs<ObserveResult.Applied>(result)
    }

    private fun evaluate(
        subject: String,
        subjectType: EntityType,
        predicate: Predicate,
        objectText: String,
        objectType: EntityType,
        at: Instant,
        eventStartAt: Instant,
    ) = engine.observeAndEvaluate(
        draft =
            draft(
                subject, subjectType, predicate,
                objectText, objectType, newEvidence(at).id, at,
            ),
        context =
            Context(
                now = at,
                upcomingEvents =
                    listOf(
                        ContextEvent(
                            eventId = experiment.id,
                            startAt = eventStartAt,
                        )
                    ),
            ),
        userId = user.id,
    )

    private fun draft(
        subject: String,
        subjectType: EntityType,
        predicate: Predicate,
        objectText: String,
        objectType: EntityType,
        evidenceId: kotlin.uuid.Uuid,
        at: Instant,
    ) = SemanticObservationDraft(
        subjectMention = EntityMention(subject, subjectType),
        predicate = predicate,
        objectMention = EntityMention(objectText, objectType),
        confidence = 1.0,
        evidenceIds = setOf(evidenceId),
        observedAt = at,
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
        engine.evidence.save(evidence)
        return evidence
    }

    @Test
    fun requirementRiskDisappearsAfterItemIsCarried() {

        val tonight =
            Instant.parse("2026-09-10T22:00:00Z")
        val tomorrowMorning =
            Instant.parse("2026-09-11T07:30:00Z")
        val experimentStart =
            Instant.parse("2026-09-11T08:00:00Z")

        // 初始事实：充电宝在宿舍；实验课需要充电宝
        state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "宿舍", EntityType.LOCATION, tonight,
        )
        state(
            "实验课", EntityType.EVENT, Predicates.REQUIRES,
            "充电宝", EntityType.ITEM, tonight,
        )

        // 面对即将开始的实验课，系统重新判断 → 风险与提醒出现
        val first =
            evaluate(
                "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
                "宿舍", EntityType.LOCATION,
                tomorrowMorning, experimentStart,
            )

        assertIs<ObserveResult.Applied>(first.observeResult)

        // RequirementRisk 出现
        assertTrue(
            first.propagationResults
                .flatMap { it.requirementRisks }
                .any {
                    it.eventId == experiment.id &&
                        it.requiredEntityId == powerBank.id
                },
            "充电宝不在身上且实验课即将开始，应产生针对充电宝的 RequirementRisk",
        )

        // Attention 中出现 PREPARE_ITEM 提醒
        assertTrue(
            first.visibleRecommendations
                .any { it.type == RecommendationType.PREPARE_ITEM },
            "应显示 PREPARE_ITEM 提醒",
        )

        // 现实变化："我把充电宝带在身上了"
        val carryTime =
            Instant.parse("2026-09-11T07:40:00Z")
        val second =
            evaluate(
                "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
                "我", EntityType.PERSON,
                carryTime, experimentStart,
            )

        assertIs<ObserveResult.Applied>(second.observeResult)

        // 风险消失
        assertTrue(
            second.propagationResults
                .flatMap { it.requirementRisks }
                .none { it.requiredEntityId == powerBank.id },
            "充电宝被随身携带后，针对它的 RequirementRisk 应消失",
        )

        // PREPARE_ITEM 提醒消失
        assertTrue(
            second.visibleRecommendations
                .none { it.type == RecommendationType.PREPARE_ITEM },
            "携带后 PREPARE_ITEM 提醒应消失",
        )

        // 状态已演化为 carried_by 我
        assertTrue(
            engine.currentStates().any {
                it.key.predicate == Predicates.CARRIED_BY &&
                    it.key.subjectId == powerBank.id &&
                    it.key.objectId == user.id
            },
            "充电宝应处于 carried_by 我 的活动状态",
        )
    }
}
