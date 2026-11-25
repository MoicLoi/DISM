package me.moiclathy.dism.integration

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.persistence.repository.InMemoryEvidenceRepository
import me.moiclathy.dism.evidence.EvidenceType
import me.moiclathy.dism.semantic.observation.ObservationProcessor
import me.moiclathy.dism.semantic.observation.ObservationResolutionResult
import me.moiclathy.dism.semantic.observation.ObservationResolver
import me.moiclathy.dism.semantic.observation.SemanticObservationDraft
import me.moiclathy.dism.semantic.resolution.EntityMention
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import me.moiclathy.dism.state.reducer.StateReducer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.uuid.Uuid

class DraftObservationStateFlowTest {

    @Test
    fun resolvedDraft_shouldPropagateIntoDynamicState() {
        val registry = EntityRegistry()

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

        registry.registerAll(
            listOf(powerBank, dorm, user)
        )

        val oldTime = Instant.parse("2026-09-08T07:00:00Z")

        val newTime = Instant.parse("2026-09-08T08:00:00Z")

        val oldEvidenceId = Uuid.random()
        val newEvidenceId = Uuid.random()

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

        var states = listOf(
            RelationState(
                key = RelationKey(
                    subjectId = powerBank.id,
                    predicate = Predicates.LOCATED_AT,
                    objectId = dorm.id,
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
        )

        val draft = SemanticObservationDraft(
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
            evidenceIds = setOf(newEvidenceId),
            observedAt = newTime,
        )

        val observationResolution = ObservationResolver()
            .resolve(
                draft = draft,
                registry = registry,
            )

        val resolved = assertIs<ObservationResolutionResult.Resolved>(
            observationResolution
        )

        val observationResult = ObservationProcessor()
            .process(resolved.observation)

        val reducer = StateReducer()

        observationResult.transitions.forEach { transition ->
            states = reducer.apply(
                states = states,
                transition = transition,
                evidenceRegistry = evidenceRegistry,
            ).currentStates
        }

        val oldLocation = states.first {
            it.key.predicate == Predicates.LOCATED_AT
        }

        val carried = states.first {
            it.key.predicate == Predicates.CARRIED_BY
        }

        assertFalse(oldLocation.isActive)

        assertEquals(
            RelationStatus.SUPERSEDED,
            oldLocation.status,
        )

        assertTrue(carried.isActive)

        assertEquals(
            user.id,
            carried.key.objectId,
        )
    }
}
