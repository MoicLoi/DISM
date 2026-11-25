package me.moiclathy.dism.semantic.observation

import me.moiclathy.dism.state.model.RelationKey
import me.moiclathy.dism.state.transition.StateTransition

/**
 * 将外部世界的观察转化为 DISM 内部状态变迁。
 *
 * Observation 表示"外部世界告诉 DISM 什么"，
 * Transition 表示"DISM 决定如何改变内部状态"。
 *
 * 两者语义层次不同，不能合并。
 * 后续 Entity Resolution、confidence policy、
 * temporal semantics、observation validation
 * 都会自然进入这一边界。
 */
class ObservationProcessor {

    fun process(
        observation: SemanticObservation,
    ): ObservationResult {

        val key = RelationKey(
            subjectId = observation.subjectId,
            predicate = observation.predicate,
            objectId = observation.objectId,
        )

        val transition = when (observation.mode) {

            ObservationMode.ASSERT ->
                StateTransition.AssertRelation(
                    key = key,
                    semanticConfidence = observation.confidence,
                    effectiveAt = observation.effectiveAt,
                    evidenceIds = observation.evidenceIds,
                )

            ObservationMode.INVALIDATE ->
                StateTransition.InvalidateRelation(
                    key = key,
                    effectiveAt = observation.effectiveAt,
                    evidenceIds = observation.evidenceIds,
                )
        }

        return ObservationResult(
            observation = observation,
            transitions = listOf(transition),
        )
    }
}
