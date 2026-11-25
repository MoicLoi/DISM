package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.state.temporal.StateChange
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * 某个语义 Transition
 * 实际导致了哪些 materialized state changes。
 *
 * 把 StateTransitionRecord（语义原因）
 * 与 TimelineChangeSet（数据结构 diff）
 * 在传播阶段重新组合。
 */
data class StateChangeEvent(
    val transitionRecord: StateTransitionRecord,
    val changes: List<StateChange>,
)
