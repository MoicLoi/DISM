package me.moiclathy.dism.persistence.db.mapper

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.moiclathy.dism.domain.entity.Entity
import me.moiclathy.dism.domain.entity.EntityType
import me.moiclathy.dism.persistence.db.Entity as DbEntity

object EntityMapper {

    @OptIn(ExperimentalUuidApi::class)
    fun toDomain(
        row: DbEntity,
    ): Entity {
        return Entity(
            id = Uuid.parse(
                row.id
            ),
            type = EntityType.valueOf(
                row.type
            ),
            name = row.name,
        )
    }
}
