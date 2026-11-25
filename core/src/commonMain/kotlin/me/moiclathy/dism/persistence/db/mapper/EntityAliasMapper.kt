package me.moiclathy.dism.persistence.db.mapper

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.moiclathy.dism.domain.entity.EntityAlias
import me.moiclathy.dism.domain.entity.EntityAliasSource
import me.moiclathy.dism.persistence.db.Entity_alias as DbEntityAlias

object EntityAliasMapper {

    @OptIn(ExperimentalUuidApi::class)
    fun toDomain(
        row: DbEntityAlias,
    ): EntityAlias {
        return EntityAlias(
            entityId = Uuid.parse(
                row.entity_id
            ),
            value = row.value_,
            source = EntityAliasSource.valueOf(
                row.source
            ),
            confidence = row.confidence,
        )
    }
}
