package me.moiclathy.dism.domain.entity

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.Uuid

class EntityRegistryTest {

    @Test
    fun registeredEntity_shouldBeRetrievableById() {
        val entity = Entity(
            type = EntityType.ITEM,
            name = "充电宝",
        )

        val registry = EntityRegistry()

        registry.register(entity)

        assertEquals(
            entity,
            registry.find(entity.id),
        )

        assertTrue(
            registry.contains(entity.id)
        )
    }

    @Test
    fun unknownEntity_shouldReturnNull() {
        val registry = EntityRegistry()

        assertNull(
            registry.find(Uuid.random())
        )
    }

    @Test
    fun registry_shouldStoreAliasesForEntity() {
        val registry = EntityRegistry()

        val campusCard = Entity(
            type = EntityType.ITEM,
            name = "校园卡",
        )

        registry.register(campusCard)

        val alias = EntityAlias(
            entityId = campusCard.id,
            value = "一卡通",
            source = EntityAliasSource.USER_DEFINED,
        )

        registry.registerAlias(alias)

        assertEquals(
            listOf(alias),
            registry.aliasesFor(campusCard.id),
        )
    }

    @Test
    fun aliasForUnknownEntity_shouldBeRejected() {
        val registry = EntityRegistry()

        assertFailsWith<IllegalArgumentException> {
            registry.registerAlias(
                EntityAlias(
                    entityId = Uuid.random(),
                    value = "未知别名",
                    source = EntityAliasSource.USER_DEFINED,
                )
            )
        }
    }
}
