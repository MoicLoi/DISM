package me.moiclathy.dism.nl.resolution

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityAlias
import me.moiclathy.dism.domain.entity.EntityAliasSource
import me.moiclathy.dism.domain.entity.EntityRegistry
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.nl.SemanticMention
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class DeterministicEntityResolverTest {
    private val registry = EntityRegistry()

    @Test
    fun exactCanonicalNameResolves() {
        val id = Uuid.parse("00000000-0000-0000-0000-000000000001")
        registry.register(Entity(id, EntityType.ITEM, "实验服"))
        val result = DeterministicEntityResolver(registry).resolve(SemanticMention("实验服"))
        assertEquals(EntityResolutionStatus.RESOLVED, result.status)
        assertEquals(id, result.entityId)
    }

    @Test
    fun aliasResolvesWithoutCreatingEntity() {
        val id = Uuid.parse("00000000-0000-0000-0000-000000000002")
        registry.register(Entity(id, EntityType.LOCATION, "宿舍"))
        registry.registerAlias(EntityAlias(id, "寝室", EntityAliasSource.USER_DEFINED))
        val result = DeterministicEntityResolver(registry).resolve(SemanticMention("寝室"))
        assertEquals(EntityResolutionStatus.RESOLVED, result.status)
        assertEquals(id, result.entityId)
        assertEquals(1, registry.all().size)
    }

    @Test
    fun closeCandidatesBecomeAmbiguous() {
        val id1 = Uuid.parse("00000000-0000-0000-0000-000000000003")
        val id2 = Uuid.parse("00000000-0000-0000-0000-000000000004")
        registry.register(Entity(id1, EntityType.ITEM, "实验服"))
        registry.register(Entity(id2, EntityType.ITEM, "实验服"))
        val result = DeterministicEntityResolver(registry).resolve(SemanticMention("实验服"))
        assertEquals(EntityResolutionStatus.AMBIGUOUS, result.status)
        assertNull(result.entityId)
    }

    @Test
    fun unknownMentionRemainsUnresolved() {
        registry.register(Entity(Uuid.parse("00000000-0000-0000-0000-000000000005"), EntityType.ITEM, "实验服"))
        val result = DeterministicEntityResolver(registry).resolve(SemanticMention("充电宝"))
        assertEquals(EntityResolutionStatus.UNRESOLVED, result.status)
    }
}
