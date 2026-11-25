package me.moiclathy.dism.semantic.engine

import me.moiclathy.dism.semantic.observation.ObservationResolutionResult
import me.moiclathy.dism.semantic.observation.SemanticObservation
import me.moiclathy.dism.state.model.RelationState

/**
 * SemanticStateEngine.observe() 的结果。
 *
 * 第一版只分：
 * - Applied: Observation 完成实体解析，并已应用到 Dynamic State
 * - Unresolved: Draft 有意义，但实体解析还不足以安全应用
 *
 * 先不要加入 Rejected / Conflict / Ignored / Deferred，
 * 等真正出现对应语义再扩展。
 */
sealed interface ObserveResult {

    /**
     * Observation 成功完成实体解析，并已经应用到 Dynamic State。
     */
    data class Applied(
        val observation: SemanticObservation,
        val appliedTransitions: List<AppliedTransition>,
        val previousStates: List<RelationState>,
        val currentStates: List<RelationState>,
    ) : ObserveResult

    /**
     * Draft 本身有意义，但实体解析还不足以安全应用。
     *
     * 例如：
     * "充电宝在宿舍"
     * 但系统同时存在东区宿舍和西区宿舍。
     */
    data class Unresolved(
        val resolution: ObservationResolutionResult.Unresolved,
        val currentStates: List<RelationState>,
    ) : ObserveResult
}
