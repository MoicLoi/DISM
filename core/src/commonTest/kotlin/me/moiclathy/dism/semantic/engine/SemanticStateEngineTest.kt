package me.moiclathy.dism.semantic.engine

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.repository.InMemoryRelationStateRepository
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import me.moiclathy.dism.semantic.resolution.EntityMentionKind
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.query.RelationQuery
import me.moiclathy.dism.state.query.TemporalScope
import me.moiclathy.dism.state.query.TemporalStateChange
import me.moiclathy.dism.state.temporal.StateChange
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class SemanticStateEngineTest {

    @Test
    fun carryingItem_shouldSupersedePreviousLocation() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm, user)
        )

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")

        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = firstTime,
            confidence = 1.0,
        )

        engine.evidence.save(firstEvidence)

        val firstResult = engine.observe(
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
                evidenceIds = setOf(firstEvidence.id),
                observedAt = firstTime,
            ),
            context = Context(now = firstTime),
        )

        assertIs<ObserveResult.Applied>(firstResult)

        val secondTime = Instant.parse("2026-09-08T08:05:00Z")

        val secondEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = secondTime,
            confidence = 1.0,
        )

        engine.evidence.save(secondEvidence)

        val secondResult = engine.observe(
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
                evidenceIds = setOf(secondEvidence.id),
                observedAt = secondTime,
            ),
            context = Context(now = secondTime),
        )

        val applied = assertIs<ObserveResult.Applied>(secondResult)

        val oldLocation = applied.currentStates.first {
            it.key.predicate == Predicates.LOCATED_AT
        }

        val carried = applied.currentStates.first {
            it.key.predicate == Predicates.CARRIED_BY
        }

        assertEquals(RelationStatus.SUPERSEDED, oldLocation.status)
        assertFalse(oldLocation.isActive)
        assertEquals(secondTime, oldLocation.validUntil)

        assertTrue(carried.isActive)
        assertEquals(user.id, carried.key.objectId)
    }

    @Test
    fun recentResolvedEntity_shouldSupportReferenceInNextObservation() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "白色充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm, user)
        )

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")

        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = firstTime,
            confidence = 1.0,
        )

        engine.evidence.save(firstEvidence)

        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "白色充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention(
                    text = "宿舍",
                    expectedType = EntityType.LOCATION,
                ),
                confidence = 1.0,
                evidenceIds = setOf(firstEvidence.id),
                observedAt = firstTime,
            ),
            context = Context(now = firstTime),
        )

        val secondTime = Instant.parse("2026-09-08T08:00:30Z")

        val secondEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = secondTime,
            confidence = 1.0,
        )

        engine.evidence.save(secondEvidence)

        val result = engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "那个",
                    expectedType = EntityType.ITEM,
                    kind = EntityMentionKind.REFERENCE,
                ),
                predicate = Predicates.CARRIED_BY,
                objectMention = EntityMention(
                    text = "我",
                    expectedType = EntityType.PERSON,
                ),
                confidence = 1.0,
                evidenceIds = setOf(secondEvidence.id),
                observedAt = secondTime,
            ),
            context = Context(now = secondTime),
        )

        val applied = assertIs<ObserveResult.Applied>(result)

        val carried = applied.currentStates.first {
            it.key.predicate == Predicates.CARRIED_BY &&
                it.isActive
        }

        assertEquals(powerBank.id, carried.key.subjectId)
    }

    @Test
    fun unresolvedObservation_shouldNotModifyState() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val eastDorm = Entity(
            type = EntityType.LOCATION,
            name = "东区宿舍",
        )

        val westDorm = Entity(
            type = EntityType.LOCATION,
            name = "西区宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, eastDorm, westDorm)
        )

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = time,
            confidence = 1.0,
        )

        // Evidence 本身真实存在，即使 Observation 最终无法落地。
        engine.evidence.save(evidence)

        val result = engine.observe(
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
                observedAt = time,
            ),
            context = Context(now = time),
        )

        assertIs<ObserveResult.Unresolved>(result)

        // Resolution 不确定时，DISM 不修改现实模型。
        assertEquals(emptyList(), engine.currentStates())
    }

    @Test
    fun observationWithUnknownEvidence_shouldBeRejected() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm)
        )

        val time = Instant.parse("2026-09-08T08:00:00Z")

        assertFailsWith<IllegalArgumentException> {
            engine.observe(
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
                    // 引用了一个 registry 里不存在的 Evidence
                    evidenceIds = setOf(Uuid.random()),
                    observedAt = time,
                ),
                context = Context(now = time),
            )
        }

        // 拒绝发生在任何状态修改之前
        assertEquals(emptyList(), engine.currentStates())
    }

    @Test
    fun appliedState_shouldRetainResolvableEvidenceChain() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm)
        )

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = time,
            confidence = 1.0,
        )

        engine.evidence.save(evidence)

        val result = engine.observe(
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
                observedAt = time,
            ),
            context = Context(now = time),
        )

        val applied = assertIs<ObserveResult.Applied>(result)

        val state = applied.currentStates.single { it.isActive }

        // 反查链：State → evidenceIds → EvidenceRegistry → 原始证据
        val supporting = engine.evidenceFor(state)

        assertEquals(listOf(evidence), supporting)
    }

    @Test
    fun repeatedEvidence_shouldAggregateIntoDynamicConfidence() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm)
        )

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")
        val secondTime = Instant.parse("2026-09-08T08:05:00Z")

        // 第一次：用户明确陈述，但语义提取置信度一般
        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = firstTime,
            confidence = 0.8,
        )

        engine.evidence.save(firstEvidence)

        val firstResult = engine.observe(
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
                confidence = 0.75,
                evidenceIds = setOf(firstEvidence.id),
                observedAt = firstTime,
            ),
            context = Context(now = firstTime),
        )

        assertIs<ObserveResult.Applied>(firstResult)

        // 第二次：另一来源（日程导入）再次支持同一关系
        val secondEvidence = Evidence(
            type = EvidenceType.SCHEDULE_IMPORT,
            source = "schedule",
            observedAt = secondTime,
            confidence = 0.7,
        )

        engine.evidence.save(secondEvidence)

        val secondResult = engine.observe(
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
                evidenceIds = setOf(secondEvidence.id),
                observedAt = secondTime,
            ),
            context = Context(now = secondTime),
        )

        val applied = assertIs<ObserveResult.Applied>(secondResult)

        val state = applied.currentStates.single { it.isActive }

        assertEquals(2, state.supports.size)

        // 0.8×0.75 = 0.60，0.7×1.0 = 0.70
        // 1 - (1 - 0.60)(1 - 0.70) = 0.88
        assertEquals(
            0.88,
            state.confidence,
            absoluteTolerance = 0.000001,
        )

        assertEquals(
            setOf(firstEvidence.id, secondEvidence.id),
            state.evidenceIds,
        )
    }

    @Test
    fun query_shouldExposeActiveRealityAndHideHistorical() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm, user)
        )

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")

        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = firstTime,
            confidence = 1.0,
        )

        engine.evidence.save(firstEvidence)

        engine.observe(
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
                evidenceIds = setOf(firstEvidence.id),
                observedAt = firstTime,
            ),
            context = Context(now = firstTime),
        )

        val secondTime = Instant.parse("2026-09-08T08:05:00Z")

        val secondEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = secondTime,
            confidence = 1.0,
        )

        engine.evidence.save(secondEvidence)

        engine.observe(
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
                evidenceIds = setOf(secondEvidence.id),
                observedAt = secondTime,
            ),
            context = Context(now = secondTime),
        )

        // 当前携带关系：可见
        val carried = engine.queryFirst(
            RelationQuery(
                subjectId = powerBank.id,
                predicate = Predicates.CARRIED_BY,
            )
        )

        assertTrue(carried != null)
        assertTrue(carried.isActive)
        assertEquals(user.id, carried.key.objectId)

        // 旧的 located_at 已被 supersede：默认 Current Reality 不可见
        assertEquals(
            null,
            engine.locatedAt(powerBank.id),
        )

        // 显式查历史才看得到
        assertEquals(
            2,
            engine.query(
                RelationQuery(
                    subjectId = powerBank.id,
                    temporalScope = TemporalScope.All,
                )
            ).size,
        )

        // convenience query
        assertEquals(
            user.id,
            engine.carriedBy(powerBank.id)?.key?.objectId,
        )
    }

    @Test
    fun explain_shouldReturnAllSupportingEvidence() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm)
        )

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")
        val secondTime = Instant.parse("2026-09-08T08:12:00Z")

        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = firstTime,
            confidence = 0.8,
        )

        engine.evidence.save(firstEvidence)

        engine.observe(
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
                confidence = 0.75,
                evidenceIds = setOf(firstEvidence.id),
                observedAt = firstTime,
            ),
            context = Context(now = firstTime),
        )

        val secondEvidence = Evidence(
            type = EvidenceType.CHECKLIST_COMPLETION,
            source = "checklist",
            observedAt = secondTime,
            confidence = 0.7,
        )

        engine.evidence.save(secondEvidence)

        engine.observe(
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
                evidenceIds = setOf(secondEvidence.id),
                observedAt = secondTime,
            ),
            context = Context(now = secondTime),
        )

        val state = engine.queryFirst(
            RelationQuery(
                subjectId = powerBank.id,
                predicate = Predicates.LOCATED_AT,
            )
        )

        val explanation = engine.explain(
            state!!,
        )

        assertEquals(state, explanation.state)

        assertEquals(
            setOf(firstEvidence.id, secondEvidence.id),
            explanation.evidence
                .map { it.id }
                .toSet(),
        )

        // 依据中的顺序：先登记的先返回（evidenceIds 的注册顺序）
        assertEquals(firstEvidence.id, explanation.evidence[0].id)
        assertEquals(secondEvidence.id, explanation.evidence[1].id)
    }

    @Test
    fun queryAtAndHistory_shouldReplayStateTimeline() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm, user)
        )

        val locatedTime = Instant.parse("2026-09-08T07:00:00Z")
        val carriedTime = Instant.parse("2026-09-08T08:05:00Z")

        val locatedEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = locatedTime,
            confidence = 1.0,
        )

        engine.evidence.save(locatedEvidence)

        engine.observe(
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
                evidenceIds = setOf(locatedEvidence.id),
                observedAt = locatedTime,
            ),
            context = Context(now = locatedTime),
        )

        val carriedEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = carriedTime,
            confidence = 1.0,
        )

        engine.evidence.save(carriedEvidence)

        engine.observe(
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
                evidenceIds = setOf(carriedEvidence.id),
                observedAt = carriedTime,
            ),
            context = Context(now = carriedTime),
        )

        // 完整历史按成立时间排序
        val history = engine.historyOf(powerBank.id)

        assertEquals(2, history.size)
        assertEquals(Predicates.LOCATED_AT, history[0].key.predicate)
        assertEquals(locatedTime, history[0].validFrom)
        assertEquals(Predicates.CARRIED_BY, history[1].key.predicate)
        assertEquals(carriedTime, history[1].validFrom)

        // 07:30 世界模型认为它在宿舍
        val morning = engine.queryAt(
            instant = Instant.parse("2026-09-08T07:30:00Z"),
            subjectId = powerBank.id,
        )

        assertEquals(1, morning.size)
        assertEquals(dorm.id, morning.single().key.objectId)

        // 08:05 切换瞬间：只看到新状态
        val atSwitch = engine.queryAt(
            instant = carriedTime,
            subjectId = powerBank.id,
        )

        assertEquals(1, atSwitch.size)
        assertEquals(Predicates.CARRIED_BY, atSwitch.single().key.predicate)
        assertEquals(user.id, atSwitch.single().key.objectId)
    }

    @Test
    fun retroactiveObservation_shouldCreateStateFromEffectiveTime() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm)
        )

        val observedAt =
            Instant.parse("2026-09-08T10:00:00Z")

        val effectiveAt =
            Instant.parse("2026-09-07T22:47:00Z")

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = observedAt,
            confidence = 1.0,
        )

        engine.evidence.save(evidence)

        val result = engine.observe(
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
                observedAt = observedAt,
                effectiveAt = effectiveAt,
            ),
            context = Context(now = observedAt),
        )

        assertIs<ObserveResult.Applied>(result)

        val state = engine.currentStates().single()

        // 信息今天才输入，但状态从昨晚 22:47 开始
        assertEquals(effectiveAt, state.validFrom)

        // 回填前的时刻查不到
        assertEquals(
            emptyList(),
            engine.queryAt(
                instant = Instant.parse("2026-09-07T22:00:00Z"),
                subjectId = powerBank.id,
            ),
        )

        // 回填后的时刻能看到
        val after = engine.queryAt(
            instant = Instant.parse("2026-09-07T23:00:00Z"),
            subjectId = powerBank.id,
        )

        assertEquals(1, after.size)
        assertEquals(dorm.id, after.single().key.objectId)
    }

    @Test
    fun retroactiveObservation_shouldSupersedeActiveStateAtEffectiveTime() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val classroom = Entity(
            type = EntityType.LOCATION,
            name = "教学楼",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, classroom, dorm)
        )

        val classroomTime = Instant.parse("2026-09-07T22:00:00Z")

        val classroomEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = classroomTime,
            confidence = 1.0,
        )

        engine.evidence.save(classroomEvidence)

        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention(
                    text = "教学楼",
                    expectedType = EntityType.LOCATION,
                ),
                confidence = 1.0,
                evidenceIds = setOf(classroomEvidence.id),
                observedAt = classroomTime,
            ),
            context = Context(now = classroomTime),
        )

        // 第二天 10:00 才说：昨晚 22:47 放宿舍了
        val observedAt = Instant.parse("2026-09-08T10:00:00Z")
        val effectiveAt = Instant.parse("2026-09-07T22:47:00Z")

        val dormEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = observedAt,
            confidence = 1.0,
        )

        engine.evidence.save(dormEvidence)

        engine.observe(
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
                evidenceIds = setOf(dormEvidence.id),
                observedAt = observedAt,
                effectiveAt = effectiveAt,
            ),
            context = Context(now = observedAt),
        )

        val classroomState = engine.queryFirst(
            RelationQuery(
                subjectId = powerBank.id,
                predicate = Predicates.LOCATED_AT,
                temporalScope = TemporalScope.All,
            )
        )

        assertEquals(RelationStatus.SUPERSEDED, classroomState?.status)
        assertEquals(effectiveAt, classroomState?.validUntil)

        // 回填后的时刻：世界模型认为它在宿舍
        val after = engine.queryAt(
            instant = Instant.parse("2026-09-07T23:00:00Z"),
            subjectId = powerBank.id,
        )

        assertEquals(1, after.size)
        assertEquals(dorm.id, after.single().key.objectId)
    }

    @Test
    fun retroactiveAssertionPredatingActiveState_shouldRebuildTimeline() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val classroom = Entity(
            type = EntityType.LOCATION,
            name = "教学楼",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, classroom, dorm)
        )

        // 已有状态：23:30 开始充电宝在教学楼（当前 active）
        val classroomTime = Instant.parse("2026-09-07T23:30:00Z")

        val classroomEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = classroomTime,
            confidence = 1.0,
        )

        engine.evidence.save(classroomEvidence)

        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention(
                    text = "教学楼",
                    expectedType = EntityType.LOCATION,
                ),
                confidence = 1.0,
                evidenceIds = setOf(classroomEvidence.id),
                observedAt = classroomTime,
            ),
            context = Context(now = classroomTime),
        )

        // 回填到 22:47：早于冲突状态（23:30）开始。
        // TemporalReconciler 重建时间线，而不是拒绝：
        // 22:47–23:30 宿舍（SUPERSEDED）
        // 23:30–∞ 教学楼（ACTIVE）
        val observedAt = Instant.parse("2026-09-08T10:00:00Z")
        val effectiveAt = Instant.parse("2026-09-07T22:47:00Z")

        val dormEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = observedAt,
            confidence = 1.0,
        )

        engine.evidence.save(dormEvidence)

        val result = engine.observe(
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
                evidenceIds = setOf(dormEvidence.id),
                observedAt = observedAt,
                effectiveAt = effectiveAt,
            ),
            context = Context(now = observedAt),
        )

        assertIs<ObserveResult.Applied>(result)

        assertEquals(2, engine.currentStates().size)

        // 23:00（回填后、教学楼状态开始前）→ 宿舍
        val at2300 = engine.queryAt(
            instant = Instant.parse("2026-09-07T23:00:00Z"),
            subjectId = powerBank.id,
        )

        assertEquals(1, at2300.size)
        assertEquals(dorm.id, at2300.single().key.objectId)
        assertEquals(
            RelationStatus.SUPERSEDED,
            at2300.single().status,
        )
        assertEquals(classroomTime, at2300.single().validUntil)

        // 23:45（教学楼状态期间）→ 教学楼
        val at2345 = engine.queryAt(
            instant = Instant.parse("2026-09-07T23:45:00Z"),
            subjectId = powerBank.id,
        )

        assertEquals(1, at2345.size)
        assertEquals(classroom.id, at2345.single().key.objectId)
    }

    @Test
    fun timelineAudit_shouldExplainRetroactiveReconciliation() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val classroom = Entity(
            type = EntityType.LOCATION,
            name = "教学楼",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val backpack = Entity(
            type = EntityType.ITEM,
            name = "书包",
        )

        engine.entities.saveAll(
            listOf(powerBank, classroom, dorm, backpack)
        )

        // 22:00 在教学楼
        val classroomTime = Instant.parse("2026-09-08T22:00:00Z")

        val classroomEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = classroomTime,
            confidence = 1.0,
        )

        engine.evidence.save(classroomEvidence)

        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.LOCATED_AT,
                objectMention = EntityMention(
                    text = "教学楼",
                    expectedType = EntityType.LOCATION,
                ),
                confidence = 1.0,
                evidenceIds = setOf(classroomEvidence.id),
                observedAt = classroomTime,
            ),
            context = Context(now = classroomTime),
        )

        // 23:30 在背包里
        val backpackTime = Instant.parse("2026-09-08T23:30:00Z")

        val backpackEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = backpackTime,
            confidence = 1.0,
        )

        engine.evidence.save(backpackEvidence)

        engine.observe(
            draft = SemanticObservationDraft(
                subjectMention = EntityMention(
                    text = "充电宝",
                    expectedType = EntityType.ITEM,
                ),
                predicate = Predicates.INSIDE,
                objectMention = EntityMention(
                    text = "书包",
                    expectedType = EntityType.ITEM,
                ),
                confidence = 1.0,
                evidenceIds = setOf(backpackEvidence.id),
                observedAt = backpackTime,
            ),
            context = Context(now = backpackTime),
        )

        // 第二天 10:00 才补充：昨晚 22:47 在宿舍
        val observedAt = Instant.parse("2026-09-09T10:00:00Z")
        val effectiveAt = Instant.parse("2026-09-08T22:47:00Z")

        val dormEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = observedAt,
            confidence = 1.0,
        )

        engine.evidence.save(dormEvidence)

        engine.observe(
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
                evidenceIds = setOf(dormEvidence.id),
                observedAt = observedAt,
                effectiveAt = effectiveAt,
            ),
            context = Context(now = observedAt),
        )

        // 22:47 发生了什么：教室状态结束、宿舍状态开始
        val changes = engine.changesAt(effectiveAt)

        assertEquals(2, changes.size)

        val ended = changes
            .filterIsInstance<TemporalStateChange.Ended>()
            .single()

        assertEquals(classroom.id, ended.state.key.objectId)

        val started = changes
            .filterIsInstance<TemporalStateChange.Started>()
            .single()

        assertEquals(dorm.id, started.state.key.objectId)

        // 语义操作记录：一条 retroactive assertion
        val records = engine.transitionsAt(effectiveAt)

        assertEquals(1, records.size)
        assertEquals(
            setOf(dormEvidence.id),
            records.single().transition.evidenceIds,
        )

        // 为什么 22:47 发生变化：
        // 证据来自 transition log（回填那条），
        // 而不是 Classroom 原始证据
        val explanation = engine.explainChangesAt(effectiveAt)

        assertEquals(effectiveAt, explanation.instant)
        assertEquals(listOf(dormEvidence), explanation.evidence)
    }

    @Test
    fun observe_shouldExposeMaterializedChanges() {
        val engine = SemanticStateEngine()

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        val user = Entity(
            type = EntityType.PERSON,
            name = "我",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm, user)
        )

        val firstTime = Instant.parse("2026-09-08T08:00:00Z")

        val firstEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = firstTime,
            confidence = 1.0,
        )

        engine.evidence.save(firstEvidence)

        val firstResult = engine.observe(
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
                evidenceIds = setOf(firstEvidence.id),
                observedAt = firstTime,
            ),
            context = Context(now = firstTime),
        )

        val firstApplied = assertIs<ObserveResult.Applied>(firstResult)

        // 第一条观察：纯插入
        assertEquals(1, firstApplied.appliedTransitions.size)

        val firstInserted =
            assertIs<StateChange.Inserted>(
                firstApplied
                    .appliedTransitions
                    .single()
                    .changes
                    .single()
            )

        assertEquals(dorm.id, firstInserted.state.key.objectId)

        val secondTime = Instant.parse("2026-09-08T08:05:00Z")

        val secondEvidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = secondTime,
            confidence = 1.0,
        )

        engine.evidence.save(secondEvidence)

        val secondResult = engine.observe(
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
                evidenceIds = setOf(secondEvidence.id),
                observedAt = secondTime,
            ),
            context = Context(now = secondTime),
        )

        val secondApplied = assertIs<ObserveResult.Applied>(secondResult)

        // 第二条观察：宿舍状态被截短（Updated）+ 携带状态插入（Inserted）
        val secondChanges =
            secondApplied
                .appliedTransitions
                .single()
                .changes

        assertEquals(2, secondChanges.size)

        val updated = secondChanges
            .filterIsInstance<StateChange.Updated>()
            .single()

        assertEquals(dorm.id, updated.before.key.objectId)
        assertEquals(secondTime, updated.after.validUntil)

        val inserted = secondChanges
            .filterIsInstance<StateChange.Inserted>()
            .single()

        assertEquals(user.id, inserted.state.key.objectId)
    }

    @Test
    fun appliedObservation_shouldPersistToStateRepository() {
        val stateRepository =
            InMemoryRelationStateRepository()

        val engine = SemanticStateEngine(
            stateRepository = stateRepository,
        )

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val dorm = Entity(
            type = EntityType.LOCATION,
            name = "宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, dorm)
        )

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = time,
            confidence = 1.0,
        )

        engine.evidence.save(evidence)

        val result = engine.observe(
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
                observedAt = time,
            ),
            context = Context(now = time),
        )

        assertIs<ObserveResult.Applied>(result)

        // 只有真正 Applied 后才写 state repository
        val persisted = stateRepository.loadAll()

        assertEquals(1, persisted.size)
        assertEquals(dorm.id, persisted.single().key.objectId)
        assertTrue(persisted.single().isActive)
    }

    @Test
    fun unresolvedObservation_shouldNotWriteStateRepository() {
        val stateRepository =
            InMemoryRelationStateRepository()

        val engine = SemanticStateEngine(
            stateRepository = stateRepository,
        )

        val powerBank = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val eastDorm = Entity(
            type = EntityType.LOCATION,
            name = "东区宿舍",
        )

        val westDorm = Entity(
            type = EntityType.LOCATION,
            name = "西区宿舍",
        )

        engine.entities.saveAll(
            listOf(powerBank, eastDorm, westDorm)
        )

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = time,
            confidence = 1.0,
        )

        engine.evidence.save(evidence)

        val result = engine.observe(
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
                observedAt = time,
            ),
            context = Context(now = time),
        )

        assertIs<ObserveResult.Unresolved>(result)

        // Unresolved：不修改 state、不写 state repository
        assertEquals(
            emptyList(),
            stateRepository.loadAll(),
        )
    }
}
