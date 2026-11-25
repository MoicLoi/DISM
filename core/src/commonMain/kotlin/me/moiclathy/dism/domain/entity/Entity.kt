package me.moiclathy.dism.domain.entity

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * DISM 语义世界中的基本实体。
 *
 * Entity 本身只描述“某个东西是什么”，
 * 不保存诸如位置、偏好、需求等动态关系。
 *
 * 例如：
 * - 用户
 * - 充电宝
 * - 宿舍
 * - 实验课
 *
 * 它们之间的关系由 Relation / State 系统维护。
 */
@OptIn(ExperimentalUuidApi::class)
data class Entity(
    val id: Uuid = Uuid.random(),
    val type: EntityType,
    val name: String,
)