package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityAlias
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.mapper.EntityAliasMapper
import me.moiclathy.dism.persistence.db.mapper.EntityMapper
import kotlin.uuid.Uuid

/**
 * SQLDelight 实现的 EntityRepository。
 *
 * Repository 接口不变 ——
 * 这只是把 EntityRegistry 的内存实现换成 SQLite 实现。
 */
class SqlDelightEntityRepository(
    private val database: DismDatabase,
) : EntityRepository {

    private val queries =
        database.entityQueries

    override fun save(
        entity: Entity,
    ) {
        queries.insertOrReplace(
            id = entity.id.toString(),
            type = entity.type.name,
            name = entity.name,
        )
    }

    override fun find(
        id: Uuid,
    ): Entity? {
        return queries
            .selectById(
                id.toString()
            )
            .executeAsOneOrNull()
            ?.let(
                EntityMapper::toDomain
            )
    }

    override fun all(): List<Entity> {
        return queries
            .selectAll()
            .executeAsList()
            .map(
                EntityMapper::toDomain
            )
    }

    override fun aliasesFor(
        entityId: Uuid,
    ): List<EntityAlias> {
        return database.entityAliasQueries
            .selectByEntityId(
                entityId.toString()
            )
            .executeAsList()
            .map(
                EntityAliasMapper::toDomain
            )
    }
}
