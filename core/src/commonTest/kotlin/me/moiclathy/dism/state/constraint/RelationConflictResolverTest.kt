package me.moiclathy.dism.state.constraint

import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationSupport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid

class RelationConflictResolverTest {

    private val resolver = RelationConflictResolver()

    @Test
    fun carriedBy_shouldConflictWithExistingLocation() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()
        val userId = Uuid.random()

        val time = Instant.parse(
            "2026-09-08T08:00:00Z"
        )

        val locatedAtDorm = RelationState(
            key = RelationKey(
                subjectId = powerBankId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = time,
        )

        val carriedByUser = RelationState(
            key = RelationKey(
                subjectId = powerBankId,
                predicate = Predicates.CARRIED_BY,
                objectId = userId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = time,
        )

        val conflicts = resolver.findConflicts(
            states = listOf(locatedAtDorm),
            incomingState = carriedByUser,
        )

        assertEquals(
            listOf(locatedAtDorm),
            conflicts,
        )
    }

    @Test
    fun physicalRelationsOfDifferentEntities_shouldNotConflict() {
        val powerBankId = Uuid.random()
        val campusCardId = Uuid.random()

        val dormId = Uuid.random()
        val userId = Uuid.random()

        val time = Instant.parse(
            "2026-09-08T08:00:00Z"
        )

        val powerBankLocation = RelationState(
            key = RelationKey(
                subjectId = powerBankId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = time,
        )

        val campusCardCarried = RelationState(
            key = RelationKey(
                subjectId = campusCardId,
                predicate = Predicates.CARRIED_BY,
                objectId = userId,
            ),
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = time,
        )

        val conflicts = resolver.findConflicts(
            states = listOf(powerBankLocation),
            incomingState = campusCardCarried,
        )

        assertEquals(
            emptyList(),
            conflicts,
        )
    }

    @Test
    fun identicalRelation_shouldNotConflictWithItself() {
        val powerBankId = Uuid.random()
        val dormId = Uuid.random()

        val time = Instant.parse(
            "2026-09-08T08:00:00Z"
        )

        val existing = RelationState(
            key = RelationKey(
                subjectId = powerBankId,
                predicate = Predicates.LOCATED_AT,
                objectId = dormId,
            ),
            confidence = 0.8,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = time,
        )

        val incoming = RelationState(
            key = existing.key,
            confidence = 1.0,
            supports = setOf(
                RelationSupport(
                    evidenceId = Uuid.random(),
                    semanticConfidence = 1.0,
                )
            ),
            validFrom = time,
        )

        val conflicts = resolver.findConflicts(
            states = listOf(existing),
            incomingState = incoming,
        )

        assertEquals(
            emptyList(),
            conflicts,
        )
    }
}
