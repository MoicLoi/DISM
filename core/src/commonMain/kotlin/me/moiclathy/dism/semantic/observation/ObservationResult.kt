package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.state.transition.StateTransition

/**
 * ObservationProcessor 的处理结果。
 *
 * 一个 Observation 可能产生多个 StateTransition，
 * 为未来语义传播留出空间。
 */
data class ObservationResult(
    val observation: SemanticObservation,
    val transitions: List<StateTransition>,
)
