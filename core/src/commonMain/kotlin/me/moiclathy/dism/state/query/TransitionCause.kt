package me.moiclathy.dism.state.query

import me.moiclathy.dism.evidence.Evidence
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * 描述某次状态边界变化的语义原因。
 *
 * record 回答“发生了什么语义操作”，
 * evidence 回答“为什么系统接受这个操作”。
 */
data class TransitionCause(
    val record: StateTransitionRecord,
    val evidence: List<Evidence>,
)
