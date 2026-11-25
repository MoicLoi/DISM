package me.moiclathy.dism.state.transition

import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.temporal.StateChange

/**
 * 一次 Transition 应用后的结果。
 */
data class TransitionResult(
    val previousStates: List<RelationState>,
    val currentStates: List<RelationState>,
    val changes: List<StateChange> = emptyList(),
)
