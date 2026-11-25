package me.moiclathy.dism.domain.entity

import me.moiclathy.dism.persistence.repository.EntityRepository
import kotlin.uuid.Uuid

/**
 * DISM 当前使用的轻量实体注册表。
 *
 * 第一版使用内存实现。
 * 后续可以替换为 SQLite / SQLDelight Repository，
 * 但上层语义逻辑不需要因此改变。
 */
class EntityRegistry :
    EntityRepository {

    private val entities = mutableMapOf<Uuid, Entity>()

    private val aliases = mutableListOf<EntityAlias>()

    /**
     * 注册或覆盖一个实体。
     */
    override fun save(entity: Entity) {
        entities[entity.id] = entity
    }

    fun register(entity: Entity) {
        save(entity)
    }

    /**
     * 批量注册。
     */
    fun registerAll(
        entities: Iterable<Entity>,
    ) {
        entities.forEach(::register)
    }

    /**
     * 如果存在则返回实体，否则返回 null。
     */
    override fun find(
        id: Uuid,
    ): Entity? {
        return entities[id]
    }

    /**
     * 必须存在。
     *
     * 适合调用方已经确定实体应存在的场景。
     */
    fun require(
        id: Uuid,
    ): Entity {
        return requireNotNull(find(id)) {
            "Entity not found: $id"
        }
    }

    /**
     * 当前是否存在这个实体。
     */
    override fun contains(
        id: Uuid,
    ): Boolean {
        return id in entities
    }

    /**
     * 返回所有已注册的实体。
     *
     * 用于 Entity Resolution 等需要遍历候选的场景。
     * Registry 本身只做存取，不做语义解析。
     */
    override fun all(): List<Entity> {
        return entities.values.toList()
    }

    /**
     * 注册一个别名。
     *
     * 别名必须关联到已存在的实体。
     */
    fun registerAlias(alias: EntityAlias) {
        require(contains(alias.entityId)) {
            "Cannot register alias for unknown entity: ${alias.entityId}"
        }

        aliases += alias
    }

    /**
     * 返回指定实体的所有别名。
     */
    override fun aliasesFor(
        entityId: Uuid,
    ): List<EntityAlias> {
        return aliases.filter {
            it.entityId == entityId
        }
    }

    /**
     * 返回所有已注册的别名。
     */
    fun allAliases(): List<EntityAlias> {
        return aliases.toList()
    }
}
