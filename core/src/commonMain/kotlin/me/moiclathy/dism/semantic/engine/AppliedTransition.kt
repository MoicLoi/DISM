package me.moiclathy.dism.semantic.engine

import me.moiclathy.dism.state.temporal.StateChange
import me.moiclathy.dism.state.transition.StateTransitionRecord

/**
 * 一条已应用的 transition
 * + 它实际导致的 materialized state changes。
 *
 * 保留 "record → changes" 的一一对应关系，
 * 比三个并列 List（transitions / transitionRecords / changes）更干净。
 */
data class AppliedTransition(
    val record: StateTransitionRecord,
    val changes: List<StateChange>,
)
