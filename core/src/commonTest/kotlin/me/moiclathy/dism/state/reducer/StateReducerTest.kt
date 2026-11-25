package me.moiclathy.dism.state.reducer

import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import me.moiclathy.dism.state.transition.StateTransition
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class StateReducerTest {

    private val reducer = StateReducer()

    /** 注册一份 confidence = 1.0 的 Evidence，返回其 id。 */
    private fun InMemoryEvidenceRepository.registerPerfectEvidence(
        time: Instant,
    ): Uuid {
        val evidence = Evidence(
            type = EvidenceType.EXPLICIT_STATEMENT,
            source = "test",
            observedAt = time,
            confidence = 1.0,
        )
        register(evidence)
        return evidence.id
    }

    @Test
    fun supersedeRelation_shouldCloseOldStateAndCreateNewActiveState() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()
        val backpackId = Uuid.random()

        val time = Instant.parse("2026-09-08T08:00:00Z")

        val registry = InMemoryEvidenceRepository()
        val originalEvidenceId = registry.registerPerfectEvidence(time)
        val newEvidenceId = registry.registerPerfectEvidence(time)

        val originalKey = RelationKey(
            subjectId = powerBankId,
            predicate = Predicates.LOCATED_AT,
            objectId = dormId,
        )

        val newKey = RelationKey(
            subjectId = powerBankId,
            predicate = Predicates.INSIDE,
            objectId = backpackId,
        )

        val originalTime = Instant.parse("2026-09-06T22:47:00Z")
        val transitionTime = Instant.parse("2026-09-07T07:03:00Z")

        val originalState = RelationState(
            key = originalKey,
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = originalEvidenceId,
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = originalTime,
        )

        val transition = StateTransition.SupersedeRelation(
            oldKey = originalKey,
            newKey = newKey,
            semanticConfidence = 1.0,
            effectiveAt = transitionTime,
            evidenceIds = setOf(newEvidenceId),
        )

        val result = reducer.apply(
            states = listOf(originalState),
            transition = transition,
            evidenceRegistry = registry,
        )

        assertEquals(1, result.previousStates.size)
        assertEquals(2, result.currentStates.size)

        val oldState = result.currentStates.first {
            it.key == originalKey
        }

        val newState = result.currentStates.first {
            it.key == newKey
        }

        assertEquals(RelationStatus.SUPERSEDED, oldState.status)
        assertEquals(transitionTime, oldState.validUntil)
        assertFalse(oldState.isActive)

        assertEquals(RelationStatus.ACTIVE, newState.status)
        assertEquals(transitionTime, newState.validFrom)
        assertEquals(setOf(newEvidenceId), newState.evidenceIds)
        assertTrue(newState.isActive)
    }

    @Test
    fun assertRelation_shouldAutomaticallySupersedeConflictingState() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()
        val userId = Uuid.random()

        val originalTime =
            Instant.parse("2026-09-08T07:00:00Z")

        val transitionTime =
            Instant.parse("2026-09-08T08:00:00Z")

        val registry = InMemoryEvidenceRepository()
        val oldEvidenceId = registry.registerPerfectEvidence(originalTime)
        val newEvidenceId = registry.registerPerfectEvidence(transitionTime)

        val locatedAtDormKey = RelationKey(
            subjectId = powerBankId,
            predicate = Predicates.LOCATED_AT,
            objectId = dormId,
        )

        val carriedByUserKey = RelationKey(
            subjectId = powerBankId,
            predicate = Predicates.CARRIED_BY,
            objectId = userId,
        )

        val oldState = RelationState(
            key = locatedAtDormKey,
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = oldEvidenceId,
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = originalTime,
        )

        val transition =
            StateTransition.AssertRelation(
                key = carriedByUserKey,
                semanticConfidence = 1.0,
                effectiveAt = transitionTime,
                evidenceIds = setOf(newEvidenceId),
            )

        val result = reducer.apply(
            states = listOf(oldState),
            transition = transition,
            evidenceRegistry = registry,
        )

        assertEquals(
            2,
            result.currentStates.size,
        )

        val previousLocation =
            result.currentStates.first {
                it.key == locatedAtDormKey
            }

        val currentCarriedState =
            result.currentStates.first {
                it.key == carriedByUserKey
            }

        assertEquals(
            RelationStatus.SUPERSEDED,
            previousLocation.status,
        )

        assertEquals(
            transitionTime,
            previousLocation.validUntil,
        )

        assertFalse(
            previousLocation.isActive,
        )

        assertEquals(
            RelationStatus.ACTIVE,
            currentCarriedState.status,
        )

        assertEquals(
            transitionTime,
            currentCarriedState.validFrom,
        )

        assertEquals(
            setOf(newEvidenceId),
            currentCarriedState.evidenceIds,
        )

        assertTrue(
            currentCarriedState.isActive,
        )
    }

    @Test
    fun assertRelation_shouldNotSupersedePhysicalStateOfAnotherEntity() {
        val powerBankId = Uuid.random()
        val campusCardId = Uuid.random()

        val dormId = Uuid.random()
        val userId = Uuid.random()

        val time =
            Instant.parse("2026-09-08T08:00:00Z")

        val registry = InMemoryEvidenceRepository()
        val campusEvidenceId = registry.registerPerfectEvidence(time)
        val carriedEvidenceId = registry.registerPerfectEvidence(time)

        val campusCardLocation = RelationState(
            key = RelationKey(
                subjectId = campusCardId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = campusEvidenceId,
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = time,
        )

        val transition =
            StateTransition.AssertRelation(
                key = RelationKey(
                    subjectId = powerBankId,
                    predicate = Predicates.CARRIED_BY,
                    objectId = userId,
                ),
                semanticConfidence = 1.0,
                effectiveAt = time,
                evidenceIds = setOf(carriedEvidenceId),
            )

        val result = reducer.apply(
            states = listOf(campusCardLocation),
            transition = transition,
            evidenceRegistry = registry,
        )

        val preservedState =
            result.currentStates.first {
                it.key == campusCardLocation.key
            }

        assertTrue(
            preservedState.isActive,
        )

        assertEquals(
            RelationStatus.ACTIVE,
            preservedState.status,
        )
    }

    @Test
    fun assertSameRelation_shouldMergeSupportsAndRecomputeConfidence() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()

        val originalTime =
            Instant.parse("2026-09-08T07:00:00Z")

        val confirmationTime =
            Instant.parse("2026-09-08T08:00:00Z")

        val registry = InMemoryEvidenceRepository()
        val firstEvidenceId = registry.registerPerfectEvidence(originalTime)
        val secondEvidenceId = registry.registerPerfectEvidence(confirmationTime)

        val key = RelationKey(
            subjectId = powerBankId,
            predicate = Predicates.LOCATED_AT,
            objectId = dormId,
        )

        val existing = RelationState(
            key = key,
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = firstEvidenceId,
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = originalTime,
        )

        val transition =
            StateTransition.AssertRelation(
                key = key,
                semanticConfidence = 1.0,
                effectiveAt = confirmationTime,
                evidenceIds = setOf(secondEvidenceId),
            )

        val result = reducer.apply(
            states = listOf(existing),
            transition = transition,
            evidenceRegistry = registry,
        )

        assertEquals(
            1,
            result.currentStates.size,
        )

        val state = result.currentStates.single()

        assertTrue(state.isActive)

        // 两份完美支持聚合后被 policy 封顶到 0.99，
        // 而不是简单 maxOf 到 1.0。
        assertEquals(
            0.99,
            state.confidence,
        )

        assertEquals(
            setOf(
                firstEvidenceId,
                secondEvidenceId,
            ),
            state.evidenceIds,
        )

        assertEquals(
            2,
            state.supports.size,
        )

        // 原始事实成立时间不能因为再次确认而被重置。
        assertEquals(
            originalTime,
            state.validFrom,
        )
    }
}
