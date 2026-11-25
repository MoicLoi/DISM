package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class TemporalReconcilerTest {

    private val reconciler = TemporalReconciler()

    private fun InMemoryEvidenceRepository.registerPerfectEvidence(
        time: Instant,
    ): Uuid {
        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = time,
            confidence = 1.0,
        )
        register(evidence)
        return evidence.id
    }

    private fun physicalState(
        itemId: Uuid,
        objectId: Uuid,
        predicate: me.moiclathy.dism.domain.relation.Predicate,
        evidenceId: Uuid,
        validFrom: Instant,
        validUntil: Instant? = null,
        status: RelationStatus = RelationStatus.ACTIVE,
    ): RelationState {
        return RelationState(
            key = RelationKey(
                subjectId = itemId,
                predicate = predicate,
                objectId = objectId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = evidenceId,
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = validFrom,
            validUntil = validUntil,
            status = status,
        )
    }

    @Test
    fun retroactiveAssertion_shouldSplitEarlierTimeline() {
        val itemId = Uuid.random()
        val classroomId = Uuid.random()
        val dormId = Uuid.random()

        val registry = InMemoryEvidenceRepository()

        val firstEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T22:00:00Z")
        )

        val retroactiveEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-09T10:00:00Z")
        )

        val oldState = physicalState(
            itemId = itemId,
            objectId = classroomId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = firstEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:00:00Z"),
        )

        val incoming = physicalState(
            itemId = itemId,
            objectId = dormId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = retroactiveEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:47:00Z"),
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(oldState),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        val classroom = result.currentStates.first {
            it.key.objectId == classroomId
        }

        val dorm = result.currentStates.first {
            it.key.objectId == dormId
        }

        // 旧状态被截断在回填时刻
        assertEquals(
            Instant.parse("2026-09-08T22:47:00Z"),
            classroom.validUntil,
        )
        assertEquals(RelationStatus.SUPERSEDED, classroom.status)

        // 新状态从回填时刻开始并保持 active
        assertEquals(
            Instant.parse("2026-09-08T22:47:00Z"),
            dorm.validFrom,
        )
        assertTrue(dorm.isActive)
    }

    @Test
    fun retroactiveAssertion_shouldPreserveLaterState() {
        val itemId = Uuid.random()
        val classroomId = Uuid.random()
        val dormId = Uuid.random()
        val backpackId = Uuid.random()

        val registry = InMemoryEvidenceRepository()

        val classroomEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T22:00:00Z")
        )

        val backpackEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T23:30:00Z")
        )

        val retroactiveEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-09T10:00:00Z")
        )

        // 已有：22:00 Classroom（23:30 被替换），23:30 Backpack（active）
        val classroom = physicalState(
            itemId = itemId,
            objectId = classroomId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = classroomEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:00:00Z"),
            validUntil = Instant.parse("2026-09-08T23:30:00Z"),
            status = RelationStatus.SUPERSEDED,
        )

        val backpack = physicalState(
            itemId = itemId,
            objectId = backpackId,
            predicate = Predicates.INSIDE,
            evidenceId = backpackEvidenceId,
            validFrom = Instant.parse("2026-09-08T23:30:00Z"),
        )

        // 回填：22:47 Dorm
        val incoming = physicalState(
            itemId = itemId,
            objectId = dormId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = retroactiveEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:47:00Z"),
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(classroom, backpack),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        val dormState = result.currentStates.first {
            it.key.objectId == dormId
        }

        // 正确结果：
        // 22:00–22:47 Classroom
        // 22:47–23:30 Dorm
        // 23:30–∞ Backpack
        assertEquals(
            Instant.parse("2026-09-08T23:30:00Z"),
            dormState.validUntil,
        )
        assertEquals(RelationStatus.SUPERSEDED, dormState.status)

        val backpackState = result.currentStates.first {
            it.key.objectId == backpackId
        }

        // 新插入状态不能把 23:30 后面的状态干掉
        assertTrue(backpackState.isActive)
    }

    @Test
    fun retroactiveAssertion_shouldAbsorbDuplicateRelation() {
        val itemId = Uuid.random()
        val classroomId = Uuid.random()
        val dormId = Uuid.random()

        val registry = InMemoryEvidenceRepository()

        val classroomEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T22:00:00Z")
        )

        val laterEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T23:30:00Z")
        )

        val retroactiveEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-09T10:00:00Z")
        )

        // 已有：22:00 Classroom，23:30 Dorm（active）
        val classroom = physicalState(
            itemId = itemId,
            objectId = classroomId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = classroomEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:00:00Z"),
            validUntil = Instant.parse("2026-09-08T23:30:00Z"),
            status = RelationStatus.SUPERSEDED,
        )

        val laterDorm = physicalState(
            itemId = itemId,
            objectId = dormId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = laterEvidenceId,
            validFrom = Instant.parse("2026-09-08T23:30:00Z"),
        )

        // 回填：22:47 Dorm —— 与 23:30 Dorm 是同一 Relation，应被吸收
        val incoming = physicalState(
            itemId = itemId,
            objectId = dormId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = retroactiveEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:47:00Z"),
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(classroom, laterDorm),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        // 时间线收敛为：
        // 22:00–22:47 Classroom
        // 22:47–∞ Dorm
        // 而不是留下重复的 23:30 Dorm 节点
        val dormStates = result.currentStates.filter {
            it.key.objectId == dormId &&
                it.key.predicate == Predicates.LOCATED_AT
        }

        assertEquals(1, dormStates.size)

        assertEquals(
            setOf(
                retroactiveEvidenceId,
                laterEvidenceId,
            ),
            dormStates.single().evidenceIds,
        )

        assertTrue(dormStates.single().isActive)
    }

    @Test
    fun reconciliation_shouldReportTimelineDiff() {
        val itemId = Uuid.random()
        val classroomId = Uuid.random()
        val dormId = Uuid.random()

        val registry = InMemoryEvidenceRepository()

        val firstEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T22:00:00Z")
        )

        val retroactiveEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-09T10:00:00Z")
        )

        val oldState = physicalState(
            itemId = itemId,
            objectId = classroomId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = firstEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:00:00Z"),
        )

        val incoming = physicalState(
            itemId = itemId,
            objectId = dormId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = retroactiveEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:47:00Z"),
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(oldState),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        val changes = result.changeSet.changes

        // Classroom 被截短 + Dorm 被插入
        assertEquals(2, changes.size)

        val updated = changes
            .filterIsInstance<StateChange.Updated>()
            .single()

        assertEquals(classroomId, updated.before.key.objectId)
        assertEquals(
            Instant.parse("2026-09-08T22:47:00Z"),
            updated.after.validUntil,
        )
        assertEquals(RelationStatus.SUPERSEDED, updated.after.status)

        val inserted = changes
            .filterIsInstance<StateChange.Inserted>()
            .single()

        assertEquals(dormId, inserted.state.key.objectId)
        assertEquals(
            Instant.parse("2026-09-08T22:47:00Z"),
            inserted.state.validFrom,
        )
    }

    @Test
    fun reconciliation_shouldNotReportUnchangedLaterState() {
        val itemId = Uuid.random()
        val classroomId = Uuid.random()
        val dormId = Uuid.random()
        val backpackId = Uuid.random()

        val registry = InMemoryEvidenceRepository()

        val classroomEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T22:00:00Z")
        )

        val backpackEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-08T23:30:00Z")
        )

        val retroactiveEvidenceId = registry.registerPerfectEvidence(
            Instant.parse("2026-09-09T10:00:00Z")
        )

        val classroom = physicalState(
            itemId = itemId,
            objectId = classroomId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = classroomEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:00:00Z"),
            validUntil = Instant.parse("2026-09-08T23:30:00Z"),
            status = RelationStatus.SUPERSEDED,
        )

        val backpack = physicalState(
            itemId = itemId,
            objectId = backpackId,
            predicate = Predicates.INSIDE,
            evidenceId = backpackEvidenceId,
            validFrom = Instant.parse("2026-09-08T23:30:00Z"),
        )

        val incoming = physicalState(
            itemId = itemId,
            objectId = dormId,
            predicate = Predicates.LOCATED_AT,
            evidenceId = retroactiveEvidenceId,
            validFrom = Instant.parse("2026-09-08T22:47:00Z"),
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(classroom, backpack),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        // diff 只描述真正受影响的现实：
        // Classroom updated、Dorm inserted、Backpack 完全不在其中
        assertTrue(
            result.changeSet.changes.none { change ->
                when (change) {
                    is StateChange.Inserted ->
                        change.state.key.objectId == backpackId

                    is StateChange.Updated ->
                        change.after.key.objectId == backpackId

                    is StateChange.Removed ->
                        change.state.key.objectId == backpackId
                }
            }
        )
    }
}
