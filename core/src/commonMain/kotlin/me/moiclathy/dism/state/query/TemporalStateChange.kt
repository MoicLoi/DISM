package me.moiclathy.dism.state.query

import me.moiclathy.dism.state.model.RelationState

/**
 * 某个时间点发生的状态切换。
 */
sealed interface TemporalStateChange {

    data class Started(
        val state: RelationState,
    ) : TemporalStateChange

    data class Ended(
        val state: RelationState,
    ) : TemporalStateChange
}
