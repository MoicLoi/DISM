package me.moiclathy.dism.state.temporal

import me.moiclathy.dism.state.model.RelationState

/**
 * Temporal Reconciliation 的结果。
 *
 * - changeSet：数据结构具体改了什么（materialized state diff）
 * - affectedStates：受影响的原始状态（audit / explain / UI diff / propagation）
 */
data class TemporalReconciliationResult(
    val previousStates: List<RelationState>,
    val currentStates: List<RelationState>,
    val affectedStates: List<RelationState>,
    val changeSet: TimelineChangeSet,
)
