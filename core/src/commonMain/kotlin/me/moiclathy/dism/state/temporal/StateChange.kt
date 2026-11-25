package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.state.model.RelationState

/**
 * 一条具体的数据结构变更。
 */
sealed interface StateChange {

    data class Inserted(
        val state: RelationState,
    ) : StateChange

    data class Updated(
        val before: RelationState,
        val after: RelationState,
    ) : StateChange

    data class Removed(
        val state: RelationState,
    ) : StateChange
}
