package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import me.moiclathy.dism.state.transition.StateTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class ObservationProcessorTest {

    private val processor = ObservationProcessor()

    @Test
    fun assertObservation_shouldProduceAssertTransition() {
        val powerBankId = Uuid.random()
        val userId = Uuid.random()
        val evidenceId = Uuid.random()

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val observation = SemanticObservation(
            subjectId = powerBankId,
            predicate = Predicates.CARRIED_BY,
            objectId = userId,
            mode = ObservationMode.ASSERT,
            confidence = 0.95,
            evidenceIds = setOf(evidenceId),
            observedAt = time,
        )

        val result = processor.process(observation)

        assertEquals(observation, result.observation)
        assertEquals(1, result.transitions.size)

        val transition = assertIs<StateTransition.AssertRelation>(
            result.transitions.single()
        )

        assertEquals(powerBankId, transition.key.subjectId)
        assertEquals(Predicates.CARRIED_BY, transition.key.predicate)
        assertEquals(userId, transition.key.objectId)
        assertEquals(0.95, transition.semanticConfidence)
        assertEquals(setOf(evidenceId), transition.evidenceIds)
        assertEquals(time, transition.effectiveAt)
    }

    @Test
    fun invalidateObservation_shouldProduceInvalidateTransition() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()
        val evidenceId = Uuid.random()

        val time = Instant.parse("2026-09-08T09:00:00Z")

        val observation = SemanticObservation(
            subjectId = powerBankId,
            predicate = Predicates.LOCATED_AT,
            objectId = dormId,
            mode = ObservationMode.INVALIDATE,
            confidence = 1.0,
            evidenceIds = setOf(evidenceId),
            observedAt = time,
        )

        val result = processor.process(observation)

        assertEquals(observation, result.observation)
        assertEquals(1, result.transitions.size)

        val transition = assertIs<StateTransition.InvalidateRelation>(
            result.transitions.single()
        )

        assertEquals(powerBankId, transition.key.subjectId)
        assertEquals(Predicates.LOCATED_AT, transition.key.predicate)
        assertEquals(dormId, transition.key.objectId)
        assertEquals(setOf(evidenceId), transition.evidenceIds)
        assertEquals(time, transition.effectiveAt)
    }

    @Test
    fun completeStateFlow_observationShouldTriggerConflictResolution() {
        // 场景：07:00 充电宝在宿舍，08:00 用户携带充电宝
        // 预期：旧位置 SUPERSEDED，新携带关系 ACTIVE

        val powerBankId = Uuid.random()
        val dormId = Uuid.random()
        val userId = Uuid.random()

        val oldEvidenceId = Uuid.random()
        val newEvidenceId = Uuid.random()

        val oldTime = Instant.parse("2026-09-08T07:00:00Z")
        val newTime = Instant.parse("2026-09-08T08:00:00Z")

        val evidenceRegistry = InMemoryEvidenceRepository().apply {
            register(
                Evidence(
                    id = oldEvidenceId,
                    type = EvidenceType.EXPLICIT_STATEMENT,
                    source = "test",
                    observedAt = oldTime,
                    confidence = 1.0,
                )
            )
            register(
                Evidence(
                    id = newEvidenceId,
                    type = EvidenceType.EXPLICIT_STATEMENT,
                    source = "test",
                    observedAt = newTime,
                    confidence = 1.0,
                )
            )
        }

        // 初始状态：充电宝在宿舍
        val initialState = me.moiclathy.dism.state.model.RelationState(
            key = me.moiclathy.dism.state.model.RelationKey(
                subjectId = powerBankId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = oldEvidenceId,
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = oldTime,
        )

        var states = listOf(initialState)

        // 观察：用户携带充电宝
        val observation = SemanticObservation(
            subjectId = powerBankId,
            predicate = Predicates.CARRIED_BY,
            objectId = userId,
            mode = ObservationMode.ASSERT,
            confidence = 1.0,
            evidenceIds = setOf(newEvidenceId),
            observedAt = newTime,
        )

        // Observation → Transition
        val observationResult = processor.process(observation)
        assertEquals(1, observationResult.transitions.size)

        // Transition → State
        val reducer = me.moiclathy.dism.state.reducer.StateReducer()
        observationResult.transitions.forEach { transition ->
            states = reducer.apply(
                states,
                transition,
                evidenceRegistry,
            ).currentStates
        }

        // 验证最终状态
        assertEquals(2, states.size)

        val oldLocation = states.first {
            it.key.predicate == Predicates.LOCATED_AT
        }
        val carriedState = states.first {
            it.key.predicate == Predicates.CARRIED_BY
        }

        // 旧位置应该被 SUPERSEDED
        assertEquals(RelationStatus.SUPERSEDED, oldLocation.status)
        assertTrue(!oldLocation.isActive)
        assertEquals(newTime, oldLocation.validUntil)
        assertEquals(setOf(oldEvidenceId), oldLocation.evidenceIds)

        // 新携带关系应该 ACTIVE
        assertEquals(RelationStatus.ACTIVE, carriedState.status)
        assertTrue(carriedState.isActive)
        assertEquals(newTime, carriedState.validFrom)
        assertEquals(userId, carriedState.key.objectId)
        assertEquals(setOf(newEvidenceId), carriedState.evidenceIds)
    }
}
