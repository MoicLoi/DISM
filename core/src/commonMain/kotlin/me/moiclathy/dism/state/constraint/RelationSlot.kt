package me.moiclathy.dism.state.constraint

/**
 * 表示一组在同一时刻通常只能存在一个当前值的关系。
 *
 * 例如：
 *
 * located_at / inside / carried_by
 *
 * 都描述一个实体当前的物理归属状态。
 */
enum class RelationSlot {
    PHYSICAL_LOCATION,
}
