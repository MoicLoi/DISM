package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

/**
 * Temporal / Conflict 边界测试。
 *
 * 这些测试锁定两个重要不变量：
 * 1. 逆序到达的事实必须正确插入时间线；
 * 2. 同一 effectiveAt 的冲突由最后进入的 assertion 获得最终解释权；
 * 3. 同 key 的同时间事实只合并 evidence，不产生重复节点。
 */
class TemporalConflictEdgeCaseTest {

    private val reconciler = TemporalReconciler()

    private fun evidence(
        registry: InMemoryEvidenceRepository,
        time: Instant,
    ): Uuid {
        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "user",
            observedAt = time,
            confidence = 1.0,
        )
        registry.register(evidence)
        return evidence.id
    }

    private fun state(
        itemId: Uuid,
        objectId: Uuid,
        predicate: me.moiclathy.dism.domain.relation.Predicate,
        evidenceId: Uuid,
        from: Instant,
        until: Instant? = null,
        status: RelationStatus = RelationStatus.ACTIVE,
    ) = RelationState(
        key = RelationKey(itemId, predicate, objectId),
        confidence = 1.0,
        supports = setOf(RelationSupport(evidenceId, 1.0)),
        validFrom = from,
        validUntil = until,
        status = status,
    )

    @Test
    fun reverseArrival_lateStateAlreadyExists_thenEarlierCorrectionSplitsTimeline() {
        val item = Uuid.random()
        val dorm = Uuid.random()
        val lab = Uuid.random()
        val registry = InMemoryEvidenceRepository()

        val lateEvidence = evidence(
            registry,
            Instant.parse("2026-09-10T23:30:00Z"),
        )
        val correctionEvidence = evidence(
            registry,
            Instant.parse("2026-09-11T10:00:00Z"),
        )

        // 数据到达顺序是：23:30 Laboratory 先到。
        val late = state(
            item,
            lab,
            Predicates.LOCATED_AT,
            lateEvidence,
            Instant.parse("2026-09-10T23:30:00Z"),
        )

        val incoming = state(
            item,
            dorm,
            Predicates.LOCATED_AT,
            correctionEvidence,
            Instant.parse("2026-09-10T22:47:00Z"),
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(late),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        val dormState = result.currentStates.single {
            it.key.objectId == dorm
        }
        val labState = result.currentStates.single {
            it.key.objectId == lab
        }

        assertEquals(
            Instant.parse("2026-09-10T23:30:00Z"),
            dormState.validUntil,
        )
        assertEquals(RelationStatus.SUPERSEDED, dormState.status)
        assertTrue(labState.isActive)

        assertEquals(2, result.currentStates.size)
    }

    @Test
    fun sameEffectiveTime_conflict_incomingWins_andOldStateIsZeroLengthHistory() {
        val item = Uuid.random()
        val dorm = Uuid.random()
        val lab = Uuid.random()
        val time = Instant.parse("2026-09-11T08:00:00Z")
        val registry = InMemoryEvidenceRepository()

        val oldEvidence = evidence(registry, time)
        val incomingEvidence = evidence(registry, time)

        val old = state(
            item,
            dorm,
            Predicates.LOCATED_AT,
            oldEvidence,
            time,
        )
        val incoming = state(
            item,
            lab,
            Predicates.LOCATED_AT,
            incomingEvidence,
            time,
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(old),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        val oldResult = result.currentStates.single {
            it.key.objectId == dorm
        }
        val newResult = result.currentStates.single {
            it.key.objectId == lab
        }

        assertEquals(RelationStatus.SUPERSEDED, oldResult.status)
        assertEquals(time, oldResult.validUntil)
        assertFalse(oldResult.isValidAt(time))

        assertEquals(RelationStatus.ACTIVE, newResult.status)
        assertTrue(newResult.isValidAt(time))
    }

    @Test
    fun sameEffectiveTime_sameRelation_mergesEvidenceWithoutDuplicateState() {
        val item = Uuid.random()
        val dorm = Uuid.random()
        val time = Instant.parse("2026-09-11T08:00:00Z")
        val registry = InMemoryEvidenceRepository()

        val firstEvidence = evidence(registry, time)
        val secondEvidence = evidence(registry, time)

        val old = state(
            item,
            dorm,
            Predicates.LOCATED_AT,
            firstEvidence,
            time,
        )
        val incoming = state(
            item,
            dorm,
            Predicates.LOCATED_AT,
            secondEvidence,
            time,
        )

        val result = reconciler.reconcileAssertion(
            states = listOf(old),
            incoming = incoming,
            evidenceRegistry = registry,
        )

        val dormStates = result.currentStates.filter {
            it.key.objectId == dorm &&
                it.key.predicate == Predicates.LOCATED_AT
        }

        assertEquals(1, dormStates.size)
        assertTrue(dormStates.single().isActive)
        assertEquals(
            setOf(firstEvidence, secondEvidence),
            dormStates.single().evidenceIds,
        )
    }
}
