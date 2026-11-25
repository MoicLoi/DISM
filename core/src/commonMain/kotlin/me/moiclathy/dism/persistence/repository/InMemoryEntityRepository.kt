package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityAlias
import kotlin.uuid.Uuid

/**
 * 13.3
 *
 * EntityRepository 的内存实现。
 *
 * 它的存在让 [me.moiclathy.dism.persistence.DismPersistence]
 * 可以完全不带数据库构造：
 *
 * ```kotlin
 * val persistence = DismPersistence()
 * ```
 *
 * 这正是 13.3 想要的 Port/Adapter 语义：
 * Core 只看到 Port，InMemory 与 SQLDelight 是可互换的 Adapter。
 *
 * 注意：与 SqlDelightEntityRepository 一样，
 * 这里也必须显式 [saveAlias] 才会写别名——
 * 5.x 层目前没有任何组件主动写 EntityAlias，
 * 这是既有行为，不在 13.3 的范围内改动。
 */
class InMemoryEntityRepository : EntityRepository {

    private val entities =
        LinkedHashMap<Uuid, Entity>()

    private val aliases =
        LinkedHashMap<Uuid, MutableList<EntityAlias>>()

    override fun save(
        entity: Entity,
    ) {
        entities[entity.id] = entity
    }

    override fun find(
        id: Uuid,
    ): Entity? =
        entities[id]

    override fun all(): List<Entity> =
        entities.values.toList()

    override fun aliasesFor(
        entityId: Uuid,
    ): List<EntityAlias> =
        aliases[entityId]
            ?.toList()
            .orEmpty()

    /**
     * 为某个实体追加一个别名。
     *
     * 与 SQL 实现保持同样的语义：别名是独立写入的，
     * 不会因为 [save] 而自动生成。
     */
    fun saveAlias(
        alias: EntityAlias,
    ) {
        aliases
            .getOrPut(alias.entityId) {
                mutableListOf()
            }
            .add(alias)
    }

    fun clear() {
        entities.clear()
        aliases.clear()
    }

    val size: Int
        get() = entities.size
}
