package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DeterministicDynamicScheduleContextBinder :
    DynamicScheduleContextBinder {

    override fun bind(
        input: DynamicScheduleContextBindingInput,
    ): DynamicScheduleContextBindingResult {

        val matches = input.model.items.map { item ->
            val itemBindings = input.bindings.forItem(item.id)

            evaluate(
                itemId = item.id,
                bindings = itemBindings,
                activeContextKeys = input.activeContextKeys,
                policy = input.policy,
            )
        }

        val changes = input.model.items.mapNotNull { item ->
            val match = matches.first { it.itemId == item.id }

            val nextState = projectState(
                currentState = item.state,
                match = match,
                policy = input.policy,
            )

            if (nextState == item.state) {
                null
            } else {
                DynamicScheduleContextBindingChange(
                    itemId = item.id,
                    previousState = item.state,
                    projectedState = nextState,
                    reason = determineChangeReason(
                        match = match,
                        previousState = item.state,
                    ),
                    match = match,
                )
            }
        }

        val nextItems = input.model.items.map { item ->
            val match = matches.first { it.itemId == item.id }

            val nextState = projectState(
                currentState = item.state,
                match = match,
                policy = input.policy,
            )

            if (nextState == item.state) {
                item
            } else {
                item.copy(
                    state = nextState,
                )
            }
        }

        val nextModel =
            if (changes.isEmpty()) {
                input.model
            } else {
                input.model.copy(
                    items = nextItems,
                    revision = input.model.revision + 1,
                )
            }

        return DynamicScheduleContextBindingResult(
            inputModel = input.model,
            projectedModel = nextModel,
            matches = matches,
            changes = changes,
        )
    }

    private fun evaluate(
        itemId: DynamicScheduleItemId,
        bindings: List<DynamicScheduleContextBinding>,
        activeContextKeys: Set<String>,
        policy: DynamicScheduleContextBindingPolicy,
    ): DynamicScheduleContextMatch {

        val relevant = bindings.filter {
            it.effectiveWeight >= policy.minimumEffectiveConfidence
        }

        val required = relevant.filter {
            it.mode == DynamicScheduleContextBindingMode.REQUIRED
        }

        val preferred = relevant.filter {
            it.mode == DynamicScheduleContextBindingMode.PREFERRED
        }

        val excluded = relevant.filter {
            it.mode == DynamicScheduleContextBindingMode.EXCLUDED
        }

        val matchedRequired = required.filter {
            it.contextKey in activeContextKeys
        }

        val unmatchedRequired = required.filter {
            it.contextKey !in activeContextKeys
        }

        val matchedExcluded = excluded.filter {
            it.contextKey in activeContextKeys
        }

        val preferredScore =
            if (preferred.isEmpty()) {
                1.0
            } else {
                val totalWeight = preferred
                    .sumOf { it.effectiveWeight }
                    .coerceAtLeast(1e-9)

                val matchedWeight = preferred.sumOf {
                    if (it.contextKey in activeContextKeys) {
                        it.effectiveWeight
                    } else {
                        0.0
                    }
                }

                matchedWeight / totalWeight
            }

        val matchedContextKeys =
            (
                matchedRequired +
                    preferred.filter {
                        it.contextKey in activeContextKeys
                    } +
                    matchedExcluded
                )
                .map { it.contextKey }
                .toSet()

        return DynamicScheduleContextMatch(
            itemId = itemId,

            requiredSatisfied =
                matchedRequired.size == required.size,

            excludedSatisfied =
                matchedExcluded.isNotEmpty(),

            preferredScore =
                preferredScore.coerceIn(0.0, 1.0),

            matchedContextKeys =
                matchedContextKeys,

            unmatchedRequiredContextKeys =
                unmatchedRequired
                    .map { it.contextKey }
                    .toSet(),

            matchedExcludedContextKeys =
                matchedExcluded
                    .map { it.contextKey }
                    .toSet(),
        )
    }

    private fun projectState(
        currentState: DynamicScheduleState,
        match: DynamicScheduleContextMatch,
        policy: DynamicScheduleContextBindingPolicy,
    ): DynamicScheduleState {

        /*
         * Explicitly blocked state has precedence over a semantic
         * context projection when the policy requests preservation.
         */
        if (
            policy.preserveExplicitBlockedState &&
            currentState == DynamicScheduleState.BLOCKED
        ) {
            return currentState
        }

        /*
         * Missing required context means this item cannot currently
         * proceed.
         */
        if (!match.requiredSatisfied) {
            return DynamicScheduleState.BLOCKED
        }

        /*
         * An excluded context actively suppresses this item.
         */
        if (match.excludedSatisfied) {
            return DynamicScheduleState.BLOCKED
        }

        /*
         * If context requirements are now satisfied and the previous
         * state was BLOCKED, the item can return to READY.
         *
         * This is only reachable when preserveExplicitBlockedState=false.
         */
        if (currentState == DynamicScheduleState.BLOCKED) {
            return DynamicScheduleState.READY
        }

        return currentState
    }

    private fun determineChangeReason(
        match: DynamicScheduleContextMatch,
        previousState: DynamicScheduleState,
    ): DynamicScheduleContextBindingChangeReason {

        return when {
            !match.requiredSatisfied ->
                DynamicScheduleContextBindingChangeReason
                    .REQUIRED_CONTEXT_MISSING

            match.excludedSatisfied ->
                DynamicScheduleContextBindingChangeReason
                    .EXCLUDED_CONTEXT_ACTIVE

            previousState == DynamicScheduleState.BLOCKED &&
                match.contextSatisfied ->
                DynamicScheduleContextBindingChangeReason
                    .CONTEXT_SATISFIED

            else ->
                DynamicScheduleContextBindingChangeReason
                    .CONTEXT_SATISFIED
        }
    }
}
