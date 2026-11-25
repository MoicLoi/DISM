package me.moiclathy.dism.state.constraint

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicate
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.semantic.engine.ObserveResult
import me.moiclathy.dism.semantic.engine.SemanticStateEngine
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant

/**
 * 冲突与纠错（Conflict / Correction）行为测试。
 *
 * 核心不变量：互斥物理槽（located_at / inside / carried_by）发生变化时，
 * 系统绝不原地 UPDATE 旧状态，而是
 *
 *   旧 State ──Temporal reconciliation──▶ SUPERSEDED + validUntil
 *   新 State ───────────────────────────▶ ACTIVE
 *
 * 因此：
 *   queryAt(旧时间) → 旧位置
 *   当前状态        → 新位置
 *   historyOf       → 能看到被取代的历史段
 *
 * 这组测试经 SemanticStateEngine 端到端驱动，
 * 真正把 RelationConflictResolver + TemporalReconciler + StateReducer 用起来。
 */
class ConflictCorrectionTest {

    private class World {
        val engine = SemanticStateEngine()

        val user = Entity(type = EntityType.PERSON, name = "我")
        val powerBank = Entity(type = EntityType.ITEM, name = "充电宝")
        val dorm = Entity(type = EntityType.LOCATION, name = "宿舍")
        val lab = Entity(type = EntityType.LOCATION, name = "实验楼")

        init {
            engine.entities.saveAll(
                listOf(user, powerBank, dorm, lab)
            )
        }

        /** 登记一句现实陈述（纯 observe，状态落地），返回该陈述的 Evidence。 */
        fun state(
            subject: String,
            subjectType: EntityType,
            predicate: Predicate,
            objectText: String,
            objectType: EntityType,
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

            val result =
                engine.observe(
                    draft =
                        SemanticObservationDraft(
                            subjectMention = EntityMention(subject, subjectType),
                            predicate = predicate,
                            objectMention = EntityMention(objectText, objectType),
                            confidence = 1.0,
                            evidenceIds = setOf(evidence.id),
                            observedAt = at,
                        ),
                    context = Context(now = at),
                )

            assertIs<ObserveResult.Applied>(result)
            return evidence
        }

        /** 当前（ACTIVE）充电宝物理归属关系。 */
        fun currentPhysical(): RelationState =
            engine.currentStates()
                .single {
                    it.key.subjectId == powerBank.id &&
                        it.isActive &&
                        (
                            it.key.predicate == Predicates.LOCATED_AT ||
                                it.key.predicate == Predicates.INSIDE ||
                                it.key.predicate == Predicates.CARRIED_BY
                            )
                }
    }

    // ── 时间锚点 ──────────────────────────────────────────────
    private val evening = Instant.parse("2026-09-10T20:00:00Z")
    private val beforeChange = Instant.parse("2026-09-10T22:00:00Z")
    private val morning = Instant.parse("2026-09-11T07:30:00Z")
    private val afterChange = Instant.parse("2026-09-11T08:00:00Z")
    private val laterMorning = Instant.parse("2026-09-11T09:00:00Z")

    @Test
    fun exclusiveSlotChange_dormToCarried_userIsCurrent() {
        val world = World()

        world.state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "宿舍", EntityType.LOCATION, evening,
        )
        world.state(
            "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
            "我", EntityType.PERSON, morning,
        )

        // 当前：充电宝 ──carried_by──▶ 我
        val current = world.currentPhysical()
        assertEquals(Predicates.CARRIED_BY, current.key.predicate)
        assertEquals(world.user.id, current.key.objectId)
        assertEquals(RelationStatus.ACTIVE, current.status)

        // 便捷读 API 也指向用户
        val carried = assertNotNull(world.engine.carriedBy(world.powerBank.id))
        assertEquals(world.user.id, carried.key.objectId)
    }

    @Test
    fun oldRelation_isNoLongerActive_andIsSupersededWithValidUntil() {
        val world = World()

        world.state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "宿舍", EntityType.LOCATION, evening,
        )
        world.state(
            "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
            "我", EntityType.PERSON, morning,
        )

        val dormState =
            world.engine.historyOf(world.powerBank.id)
                .single {
                    it.key.predicate == Predicates.LOCATED_AT &&
                        it.key.objectId == world.dorm.id
                }

        // 旧关系不再 ACTIVE，被取代且封闭于变化时刻
        assertEquals(RelationStatus.SUPERSEDED, dormState.status)
        assertEquals(morning, dormState.validUntil)
    }

    @Test
    fun supersededHistory_isPreserved() {
        val world = World()

        world.state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "宿舍", EntityType.LOCATION, evening,
        )
        world.state(
            "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
            "我", EntityType.PERSON, morning,
        )

        val history = world.engine.historyOf(world.powerBank.id)

        // 历史里仍能看到"充电宝在宿舍"这一段
        assertTrue(
            history.any {
                it.key.predicate == Predicates.LOCATED_AT &&
                    it.key.objectId == world.dorm.id &&
                    it.status == RelationStatus.SUPERSEDED
            },
            "historyOf 应保留被取代的 located_at 宿舍 段",
        )
        // 也能看到当前 carried_by 段
        assertTrue(
            history.any {
                it.key.predicate == Predicates.CARRIED_BY &&
                    it.key.objectId == world.user.id &&
                it.status == RelationStatus.ACTIVE
            },
            "historyOf 应包含当前 carried_by 我 段",
        )
    }

    @Test
    fun temporalQuery_beforeChangeReturnsDorm_afterChangeReturnsUser() {
        val world = World()

        world.state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "宿舍", EntityType.LOCATION, evening,
        )
        world.state(
            "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
            "我", EntityType.PERSON, morning,
        )

        // 变化前（半开区间 [validFrom, validUntil)）→ 宿舍
        val atBefore =
            world.engine.queryAt(
                instant = beforeChange,
                subjectId = world.powerBank.id,
            )
        assertEquals(1, atBefore.size)
        assertEquals(Predicates.LOCATED_AT, atBefore.single().key.predicate)
        assertEquals(world.dorm.id, atBefore.single().key.objectId)

        // 变化后 → 用户
        val atAfter =
            world.engine.queryAt(
                instant = afterChange,
                subjectId = world.powerBank.id,
            )
        assertEquals(1, atAfter.size)
        assertEquals(Predicates.CARRIED_BY, atAfter.single().key.predicate)
        assertEquals(world.user.id, atAfter.single().key.objectId)

        // 边界：恰好 morning（07:30）旧段已结束、新段开始
        val atBoundary =
            world.engine.queryAt(
                instant = morning,
                subjectId = world.powerBank.id,
            )
        assertEquals(world.user.id, atBoundary.single().key.objectId)
    }

    @Test
    fun duplicateSameFact_mergesEvidenceWithoutCreatingNewState() {
        val world = World()

        // 先在宿舍，再带上
        world.state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "宿舍", EntityType.LOCATION, evening,
        )
        val firstCarryEvidence =
            world.state(
                "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
                "我", EntityType.PERSON, morning,
            )

        // 再次陈述同一事实："充电宝还在我身上"
        val secondCarryEvidence =
            world.state(
                "充电宝", EntityType.ITEM, Predicates.CARRIED_BY,
                "我", EntityType.PERSON, laterMorning,
            )

        val carriedStates =
            world.engine.historyOf(world.powerBank.id)
                .filter {
                    it.key.predicate == Predicates.CARRIED_BY &&
                        it.key.objectId == world.user.id
                }

        // 不应制造第二个 carried_by 状态节点
        assertEquals(
            1,
            carriedStates.size,
            "重复同一事实应合并，而不是新建状态",
        )

        val carried = carriedStates.single()
        assertEquals(RelationStatus.ACTIVE, carried.status)
        // 起始时间保持第一次断言的时刻，不被第二次推到 laterMorning
        assertEquals(morning, carried.validFrom)
        // 两条证据都并入 supports
        assertTrue(
            carried.evidenceIds.containsAll(
                setOf(firstCarryEvidence.id, secondCarryEvidence.id)
            ),
            "重复陈述应合并 evidence",
        )
    }

    @Test
    fun explicitCorrection_dormToLaboratory_labActiveDormSuperseded() {
        val world = World()

        world.state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "宿舍", EntityType.LOCATION, evening,
        )
        world.state(
            "充电宝", EntityType.ITEM, Predicates.LOCATED_AT,
            "实验楼", EntityType.LOCATION, laterMorning,
        )

        // 当前位置 → 实验楼
        val current = world.currentPhysical()
        assertEquals(Predicates.LOCATED_AT, current.key.predicate)
        assertEquals(world.lab.id, current.key.objectId)
        assertEquals(RelationStatus.ACTIVE, current.status)

        val located = assertNotNull(world.engine.locatedAt(world.powerBank.id))
        assertEquals(world.lab.id, located.key.objectId)

        // 宿舍成为被取代的历史
        val dormState =
            world.engine.historyOf(world.powerBank.id)
                .single {
                    it.key.predicate == Predicates.LOCATED_AT &&
                        it.key.objectId == world.dorm.id
                }
        assertEquals(RelationStatus.SUPERSEDED, dormState.status)
        assertEquals(laterMorning, dormState.validUntil)

        // 时间查询：纠错前 → 宿舍；纠错后 → 实验楼
        assertEquals(
            world.dorm.id,
            world.engine
                .queryAt(beforeChange, subjectId = world.powerBank.id)
                .single()
                .key.objectId,
        )
        assertEquals(
            world.lab.id,
            world.engine
                .queryAt(laterMorning, subjectId = world.powerBank.id)
                .single()
                .key.objectId,
        )
    }
}
