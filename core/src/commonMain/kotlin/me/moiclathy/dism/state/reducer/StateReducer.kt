package me.moiclathy.dism.state.reducer

import me.moiclathy.dism.persistence.repository.EvidenceRepository
import me.moiclathy.dism.semantic.confidence.RelationConfidenceAggregator
import me.moiclathy.dism.state.constraint.RelationConflictResolver
import me.moiclathy.dism.state.constraint.RelationConstraintPolicy
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.model.RelationStatus
import me.moiclathy.dism.state.model.RelationSupport
import me.moiclathy.dism.state.temporal.TemporalReconciler
import me.moiclathy.dism.state.temporal.TimelineDiffBuilder
import me.moiclathy.dism.state.transition.StateTransition
import me.moiclathy.dism.state.transition.TransitionResult

/**
 * 将 StateTransition 应用到当前状态集合。
 *
 * RelationState.confidence 是派生值：
 * 由 RelationConfidenceAggregator 基于 supports 计算，
 * 不是调用者直接塞进来的最终答案。
 *
 * Reducer 不持有全局 EvidenceRegistry ——
 * 每次 apply() 显式传入，保持无隐藏世界状态依赖。
 *
 * 互斥槽位（PHYSICAL_LOCATION 等）的 Assertion
 * 统一交给 TemporalReconciler 做时间线重建——
 * 实时 assertion 与 retroactive assertion 共用一套逻辑。
 */
class StateReducer(
    private val confidenceAggregator: RelationConfidenceAggregator =
        RelationConfidenceAggregator(),

    private val conflictResolver: RelationConflictResolver =
        RelationConflictResolver(),

    private val constraintPolicy: RelationConstraintPolicy =
        RelationConstraintPolicy(),

    private val temporalReconciler: TemporalReconciler =
        TemporalReconciler(),

    private val diffBuilder: TimelineDiffBuilder =
        TimelineDiffBuilder(),
) {

    fun apply(
        states: List<RelationState>,
        transition: StateTransition,
        evidenceRegistry: EvidenceRepository,
    ): TransitionResult {

        return when (transition) {
            is StateTransition.AssertRelation ->
                applyAssert(states, transition, evidenceRegistry)

            is StateTransition.SupersedeRelation ->
                applySupersede(states, transition, evidenceRegistry)

            is StateTransition.InvalidateRelation ->
                applyInvalidation(states, transition)
        }
    }

    private fun applyAssert(
        states: List<RelationState>,
        transition: StateTransition.AssertRelation,
        evidenceRegistry: EvidenceRepository,
    ): TransitionResult {

        val incomingSupports =
            transition.evidenceIds
                .mapTo(mutableSetOf()) { evidenceId ->
                    RelationSupport(
                        evidenceId = evidenceId,
                        semanticConfidence = transition.semanticConfidence,
                    )
                }

        val incomingConfidence =
            confidenceAggregator.aggregate(
                supports = incomingSupports,
                evidenceRegistry = evidenceRegistry,
            )

        val incomingState =
            RelationState(
                key = transition.key,
                confidence = incomingConfidence,
                supports = incomingSupports,
                validFrom = transition.effectiveAt,
            )

        // 互斥槽位：统一交给 TemporalReconciler 重建局部时间线。
        val slot =
            constraintPolicy.slotOf(
                transition.key.predicate
            )

        if (slot != null) {
            val reconciled =
                temporalReconciler
                    .reconcileAssertion(
                        states = states,
                        incoming = incomingState,
                        evidenceRegistry = evidenceRegistry,
                    )

            return TransitionResult(
                previousStates =
                    reconciled.previousStates,
                currentStates =
                    reconciled.currentStates,
                changes =
                    reconciled.changeSet.changes,
            )
        }

        return applyNonExclusiveAssert(
            states = states,
            incomingState = incomingState,
            evidenceRegistry = evidenceRegistry,
        )
    }

    /**
     * 非互斥关系（requires 等）的普通断言：
     * 同 key 合并 supports 重算 confidence；
     * 本类关系没有冲突槽位，因此不会触发 supersede。
     */
    private fun applyNonExclusiveAssert(
        states: List<RelationState>,
        incomingState: RelationState,
        evidenceRegistry: EvidenceRepository,
    ): TransitionResult {

        val existingSameState =
            states.firstOrNull { state ->
                state.isActive &&
                    state.key == incomingState.key
            }

        if (existingSameState != null) {
            // 相同 Relation：合并 supports 后重新聚合 confidence
            val mergedSupports =
                mergeSupports(
                    existing = existingSameState.supports,
                    incoming = incomingState.supports,
                )

            val mergedConfidence =
                confidenceAggregator.aggregate(
                    supports = mergedSupports,
                    evidenceRegistry = evidenceRegistry,
                )

            val mergedState =
                existingSameState.copy(
                    confidence = mergedConfidence,
                    supports = mergedSupports,
                )

            val updatedStates =
                states.map { state ->
                    if (state === existingSameState) {
                        mergedState
                    } else {
                        state
                    }
                }

            return TransitionResult(
                previousStates = states,
                currentStates = updatedStates,
                changes = diffBuilder.build(
                    before = states,
                    after = updatedStates,
                ).changes,
            )
        }

        val conflicts =
            conflictResolver.findConflicts(
                states = states,
                incomingState = incomingState,
            )

        val conflictKeys =
            conflicts
                .map { it.key }
                .toSet()

        val updatedStates =
            states.map { state ->
                if (
                    state.isActive &&
                    state.key in conflictKeys
                ) {
                    state.copy(
                        status = RelationStatus.SUPERSEDED,
                        validUntil = incomingState.validFrom,
                    )
                } else {
                    state
                }
            }

        val currentStates =
            updatedStates + incomingState

        return TransitionResult(
            previousStates = states,
            currentStates = currentStates,
            changes = diffBuilder.build(
                before = states,
                after = currentStates,
            ).changes,
        )
    }

    private fun applySupersede(
        states: List<RelationState>,
        transition: StateTransition.SupersedeRelation,
        evidenceRegistry: EvidenceRepository,
    ): TransitionResult {

        val updatedStates =
            states.map { state ->
                if (
                    state.key == transition.oldKey &&
                    state.isActive
                ) {
                    state.copy(
                        status = RelationStatus.SUPERSEDED,
                        validUntil = transition.effectiveAt,
                    )
                } else {
                    state
                }
            }

        val newSupports =
            transition.evidenceIds
                .mapTo(mutableSetOf()) { evidenceId ->
                    RelationSupport(
                        evidenceId = evidenceId,
                        semanticConfidence = transition.semanticConfidence,
                    )
                }

        val newState =
            RelationState(
                key = transition.newKey,
                confidence = confidenceAggregator.aggregate(
                    supports = newSupports,
                    evidenceRegistry = evidenceRegistry,
                ),
                supports = newSupports,
                validFrom = transition.effectiveAt,
            )

        val currentStates =
            updatedStates + newState

        return TransitionResult(
            previousStates = states,
            currentStates = currentStates,
            changes = diffBuilder.build(
                before = states,
                after = currentStates,
            ).changes,
        )
    }

    private fun applyInvalidation(
        states: List<RelationState>,
        transition: StateTransition.InvalidateRelation,
    ): TransitionResult {

        // 注意：还不建模证据极性（contradict），
        // 因此 invalidation 证据不进入 supports；
        // 等 Evidence polarity / supersession 出现后再扩展。
        val updatedStates =
            states.map { state ->
                if (
                    state.key == transition.key &&
                    state.isActive
                ) {
                    state.copy(
                        status = RelationStatus.INVALIDATED,
                        validUntil = transition.effectiveAt,
                    )
                } else {
                    state
                }
            }

        return TransitionResult(
            previousStates = states,
            currentStates = updatedStates,
            changes = diffBuilder.build(
                before = states,
                after = updatedStates,
            ).changes,
        )
    }

    /**
     * 合并 supports：
     * 同一个 evidenceId 只保留一份 support，取最高 semanticConfidence。
     *
     * 例如同一 Evidence 被重新提交：
     * (E123, 0.7) + (E123, 0.9) → (E123, 0.9)
     */
    private fun mergeSupports(
        existing: Set<RelationSupport>,
        incoming: Set<RelationSupport>,
    ): Set<RelationSupport> {

        return (existing + incoming)
            .groupBy { it.evidenceId }
            .map { (_, supports) ->
                supports.maxBy { it.semanticConfidence }
            }
            .toSet()
    }
}
