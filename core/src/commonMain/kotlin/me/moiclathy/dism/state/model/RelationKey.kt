package me.moiclathy.dism.state.model

import me.moiclathy.dism.domain.relation.Predicate
import kotlin.uuid.Uuid

/**
 * 一个关系的稳定身份。
 *
 * 例如：
 * PowerBank --located_at--> Dorm
 */
data class RelationKey(
    val subjectId: Uuid,
    val predicate: Predicate,
    val objectId: Uuid,
)
