package me.moiclathy.dism.nl.query.resolution

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.nl.query.SemanticStateQuery
import me.moiclathy.dism.nl.query.SemanticStateQueryCandidate
import me.moiclathy.dism.nl.query.SemanticStateQueryKind
import kotlin.uuid.Uuid

class SemanticStateQueryResolutionTest {
    @Test
    fun resolvesLocationQuerySubject() {
        val entity = Entity(type = EntityType.ITEM, name = "实验服")
        val registry = EntityRegistry().apply { register(entity) }
        val candidate = SemanticStateQueryCandidate(
            query = SemanticStateQuery(queryKind = SemanticStateQueryKind.LOCATION, confidence = 0.95),
            unresolvedMentions = listOf("实验服"),
        )

        val result = DeterministicSemanticStateQueryResolver(registry).resolve(candidate)

        assertEquals(SemanticStateQueryResolutionStatus.RESOLVED, result.status)
        assertEquals(entity.id.toString(), result.query?.subjectEntityId)
        assertEquals(0.95, result.query?.confidence)
        assertNotNull(result.subjectResolution)
    }

    @Test
    fun ambiguousEntityDoesNotBecomeQuery() {
        val first = Entity(type = EntityType.ITEM, name = "实验服")
        val second = Entity(type = EntityType.ITEM, name = "实验服")
        val registry = EntityRegistry().apply { register(first); register(second) }
        val candidate = SemanticStateQueryCandidate(
            query = SemanticStateQuery(queryKind = SemanticStateQueryKind.LOCATION, confidence = 1.0),
            unresolvedMentions = listOf("实验服"),
        )

        val result = DeterministicSemanticStateQueryResolver(registry).resolve(candidate)

        assertEquals(SemanticStateQueryResolutionStatus.AMBIGUOUS, result.status)
        assertNull(result.query)
    }

    @Test
    fun unknownEntityDoesNotBecomeQuery() {
        val registry = EntityRegistry()
        val candidate = SemanticStateQueryCandidate(
            query = SemanticStateQuery(queryKind = SemanticStateQueryKind.STATE, confidence = 1.0),
            unresolvedMentions = listOf("不存在的东西"),
        )

        val result = DeterministicSemanticStateQueryResolver(registry).resolve(candidate)

        assertEquals(SemanticStateQueryResolutionStatus.UNRESOLVED, result.status)
        assertNull(result.query)
    }

    @Test
    fun emptyMentionsAreRejected() {
        val candidate = SemanticStateQueryCandidate(
            query = SemanticStateQuery(queryKind = SemanticStateQueryKind.STATE, confidence = 1.0),
        )

        val result = DeterministicSemanticStateQueryResolver(EntityRegistry()).resolve(candidate)

        assertEquals(SemanticStateQueryResolutionStatus.UNRESOLVED, result.status)
        assertNull(result.query)
    }
}
