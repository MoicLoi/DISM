package me.moiclathy.dism.semantic.propagation

import me.moiclathy.dism.context.Context
import me.moiclathy.dism.domain.relation.Predicates
import me.moiclathy.dism.state.model.RelationState
import me.moiclathy.dism.state.temporal.StateChange
import kotlin.uuid.Uuid

/**
 * 状态变化 → 哪些下游语义需要重新计算。
 *
 * PropagationEngine 使用当前 materialized state 作为最终现实来源；
 * StateChangeEvent 只决定传播范围，不自己成为 Truth Source。
 */
class SemanticPropagationEngine(
    private val requirementRiskEvaluator: RequirementRiskEvaluator =
        RequirementRiskEvaluator(),
    private val requirementRiskLifecycleReconciler: RequirementRiskLifecycleReconciler =
        RequirementRiskLifecycleReconciler(),
) {

    fun propagate(
        event: StateChangeEvent,
        states: List<RelationState>,
        context: Context,
        userId: Uuid,
        previousRequirementRisks: List<RequirementRisk> = emptyList(),
    ): PropagationResult {
        val affectedEventIds = affectedRequirementEventIds(event, states)
        if (affectedEventIds.isEmpty()) {
            return PropagationResult()
        }

        val currentRisks = affectedEventIds.flatMap { eventId ->
            requirementRiskEvaluator.evaluate(
                eventId = eventId,
                states = states,
                context = context,
                userId = userId,
            )
        }

        val previousAffectedRisks = previousRequirementRisks.filter {
            it.eventId in affectedEventIds
        }

        val lifecycleChanges =
            requirementRiskLifecycleReconciler.reconcile(
                previousRisks = previousAffectedRisks,
                currentRisks = currentRisks,
                context = context,
            )

        return PropagationResult(
            affectedEventIds = affectedEventIds,
            requirementRisks = currentRisks,
            requirementRiskLifecycleChanges = lifecycleChanges,
        )
    }

    private fun affectedRequirementEventIds(
        event: StateChangeEvent,
        states: List<RelationState>,
    ): Set<Uuid> {
        val changes = event.changes
        if (changes.isEmpty()) return emptySet()

        val activeRequirements = states.asSequence()
            .filter { it.isActive && it.key.predicate == Predicates.REQUIRES }
            .toList()

        val affected = linkedSetOf<Uuid>()

        changes.forEach { change ->
            val predicate = when (change) {
                is StateChange.Inserted -> change.state.key.predicate
                is StateChange.Updated -> change.after.key.predicate
                is StateChange.Removed -> change.state.key.predicate
            }

            when (predicate) {
                Predicates.REQUIRES -> {
                    val subjectId = when (change) {
                        is StateChange.Inserted -> change.state.key.subjectId
                        is StateChange.Updated -> change.after.key.subjectId
                        is StateChange.Removed -> change.state.key.subjectId
                    }
                    affected.add(subjectId)
                }

                Predicates.LOCATED_AT,
                Predicates.INSIDE,
                Predicates.CARRIED_BY -> {
                    val changedEntityId = when (change) {
                        is StateChange.Inserted -> change.state.key.subjectId
                        is StateChange.Updated -> change.after.key.subjectId
                        is StateChange.Removed -> change.state.key.subjectId
                    }

                    activeRequirements
                        .asSequence()
                        .filter { it.key.objectId == changedEntityId }
                        .mapTo(affected) { it.key.subjectId }
                }
            }
        }

        return affected
    }
}
