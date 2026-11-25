package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityAlias
import kotlin.uuid.Uuid

/**
 * Entity 持久化边界。
 *
 * Core 不知道底层是：
 * - MutableMap
 * - SQLite
 * - 测试 Fake
 * - 未来远程同步
 */
interface EntityRepository {

    fun save(entity: Entity)

    fun saveAll(
        entities: Iterable<Entity>,
    ) {
        entities.forEach(::save)
    }

    fun find(
        id: Uuid,
    ): Entity?

    fun all(): List<Entity>

    fun contains(
        id: Uuid,
    ): Boolean {
        return find(id) != null
    }

    /**
     * 返回指定实体的所有别名。
     * EntityAlias 属于实体持久化的一部分。
     */
    fun aliasesFor(
        entityId: Uuid,
    ): List<EntityAlias>
}
