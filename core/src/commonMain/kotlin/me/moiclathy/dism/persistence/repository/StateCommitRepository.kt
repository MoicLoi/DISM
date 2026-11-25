package me.moiclathy.dism.persistence.repository

import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * Dynamic State + Transition Log 的一次提交边界。
 *
 * 对 SQLDelight 实现而言，commit() 必须在同一个数据库事务中完成，
 * 从而避免 state 已更新而 transition log 未写入，或反之。
 */
interface StateCommitRepository {
    fun commit(
        states: List<RelationState>,
        transitions: List<StateTransitionRecord>,
    )
}
