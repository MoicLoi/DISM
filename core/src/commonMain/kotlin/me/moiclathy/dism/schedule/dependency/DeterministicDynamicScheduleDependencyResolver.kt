package me.moiclathy.dism.schedule.dependency

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DeterministicDynamicScheduleDependencyResolver : DynamicScheduleDependencyResolver {
    override fun resolve(
        input: DynamicScheduleDependencyInput,
    ): DynamicScheduleDependencyResolutionResult {
        val effectiveDependencies = input.dependencies.activeDependencies
            .filter { it.confidence >= input.policy.minimumEffectiveConfidence }
            .sortedWith(
                compareBy<DynamicScheduleDependency>(
                    { it.toItemId.value },
                    { it.fromItemId.value },
                    { it.type.name },
                    { it.id.value },
                ),
            )

        val cycles = detectCycles(input.model.items.map { it.id }, effectiveDependencies)

        val matches = effectiveDependencies.map { dependency ->
            val sourceState = input.model.item(dependency.fromItemId)?.state
                ?: DynamicScheduleState.UNSCHEDULED

            when (dependency.type) {
                DynamicScheduleDependencyType.REQUIRES_COMPLETION -> {
                    val satisfied = sourceState == DynamicScheduleState.COMPLETED
                    DynamicScheduleDependencyMatch(
                        dependency = dependency,
                        sourceState = sourceState,
                        satisfied = satisfied,
                        blocking = !satisfied && input.policy.blockOnUnmetDependency,
                        reason = if (satisfied) {
                            DynamicScheduleDependencyMatchReason.COMPLETION_SATISFIED
                        } else {
                            DynamicScheduleDependencyMatchReason.COMPLETION_REQUIRED
                        },
                    )
                }

                DynamicScheduleDependencyType.REQUIRES_START -> {
                    val satisfied = sourceState == DynamicScheduleState.ACTIVE ||
                        sourceState == DynamicScheduleState.COMPLETED
                    DynamicScheduleDependencyMatch(
                        dependency = dependency,
                        sourceState = sourceState,
                        satisfied = satisfied,
                        blocking = !satisfied && input.policy.blockOnUnmetDependency,
                        reason = if (satisfied) {
                            DynamicScheduleDependencyMatchReason.START_SATISFIED
                        } else {
                            DynamicScheduleDependencyMatchReason.START_REQUIRED
                        },
                    )
                }

                DynamicScheduleDependencyType.PRECEDES -> {
                    val satisfied = sourceState == DynamicScheduleState.COMPLETED ||
                        sourceState == DynamicScheduleState.CANCELLED ||
                        sourceState == DynamicScheduleState.EXPIRED
                    DynamicScheduleDependencyMatch(
                        dependency = dependency,
                        sourceState = sourceState,
                        satisfied = satisfied,
                        blocking = !satisfied && input.policy.blockOnUnmetDependency,
                        reason = if (satisfied) {
                            DynamicScheduleDependencyMatchReason.PRECEDENCE_SATISFIED
                        } else {
                            DynamicScheduleDependencyMatchReason.PRECEDENCE_REQUIRED
                        },
                    )
                }

                DynamicScheduleDependencyType.ASSOCIATED -> {
                    DynamicScheduleDependencyMatch(
                        dependency = dependency,
                        sourceState = sourceState,
                        satisfied = true,
                        blocking = input.policy.associatedCountsAsBlocking,
                        reason = if (input.policy.associatedCountsAsBlocking) {
                            DynamicScheduleDependencyMatchReason.ASSOCIATED_BLOCKING
                        } else {
                            DynamicScheduleDependencyMatchReason.ASSOCIATED_IGNORED
                        },
                    )
                }
            }
        }

        val unmetByItem = matches
            .filter { it.blocking && !it.satisfied }
            .groupBy { it.dependency.toItemId }

        val nextItems = input.model.items.map { item ->
            if (input.policy.preserveTerminalStates && item.state.isTerminal()) {
                item
            } else {
                val unmet = unmetByItem[item.id].orEmpty()
                when {
                    unmet.isNotEmpty() -> item.copy(state = DynamicScheduleState.BLOCKED)
                    input.policy.restoreBlockedWhenDependenciesBecomeSatisfied &&
                        item.state == DynamicScheduleState.BLOCKED -> item.copy(
                            state = DynamicScheduleState.READY,
                        )
                    else -> item
                }
            }
        }

        val changes = input.model.items.mapNotNull { previous ->
            val projected = nextItems.first { it.id == previous.id }
            if (projected.state == previous.state) {
                null
            } else {
                val unmet = unmetByItem[previous.id].orEmpty().map { it.dependency.id }
                DynamicScheduleDependencyChange(
                    itemId = previous.id,
                    previousState = previous.state,
                    projectedState = projected.state,
                    reason = if (projected.state == DynamicScheduleState.BLOCKED) {
                        DynamicScheduleDependencyChangeReason.DEPENDENCY_UNMET
                    } else {
                        DynamicScheduleDependencyChangeReason.DEPENDENCIES_SATISFIED
                    },
                    unmetDependencies = unmet,
                )
            }
        }

        val projectedModel = if (changes.isEmpty()) {
            input.model
        } else {
            input.model.copy(
                items = nextItems,
                revision = input.model.revision + 1,
            )
        }

        val warnings = buildList {
            if (cycles.isNotEmpty()) {
                add("Dependency graph contains ${cycles.size} cycle(s)")
            }
            if (changes.any { it.reason == DynamicScheduleDependencyChangeReason.DEPENDENCY_UNMET }) {
                add("${changes.count { it.reason == DynamicScheduleDependencyChangeReason.DEPENDENCY_UNMET }} item(s) blocked by dependencies")
            }
        }

        return DynamicScheduleDependencyResolutionResult(
            inputModel = input.model,
            projectedModel = projectedModel,
            matches = matches,
            changes = changes,
            topologicalOrder = topologicalOrder(input.model.items.map { it.id }, effectiveDependencies),
            cycles = cycles,
            warnings = warnings,
        )
    }

    private fun DynamicScheduleState.isTerminal(): Boolean =
        this == DynamicScheduleState.COMPLETED ||
            this == DynamicScheduleState.CANCELLED ||
            this == DynamicScheduleState.EXPIRED

    private fun topologicalOrder(
        itemIds: List<DynamicScheduleItemId>,
        dependencies: List<DynamicScheduleDependency>,
    ): List<DynamicScheduleItemId> {
        val adjacency = itemIds.associateWith { mutableSetOf<DynamicScheduleItemId>() }
        val indegree = itemIds.associateWith { 0 }.toMutableMap()

        dependencies
            .filter { it.type != DynamicScheduleDependencyType.ASSOCIATED }
            .forEach { dependency ->
                val outgoing = adjacency.getValue(dependency.fromItemId)
                if (outgoing.add(dependency.toItemId)) {
                    indegree[dependency.toItemId] = indegree.getValue(dependency.toItemId) + 1
                }
            }

        val ready = itemIds
            .filter { indegree.getValue(it) == 0 }
            .sortedBy { it.value }
            .toMutableList()
        val result = mutableListOf<DynamicScheduleItemId>()

        while (ready.isNotEmpty()) {
            ready.sortBy { it.value }
            val id = ready.removeAt(0)
            result += id
            adjacency.getValue(id)
                .sortedBy { it.value }
                .forEach { target ->
                    indegree[target] = indegree.getValue(target) - 1
                    if (indegree.getValue(target) == 0) {
                        ready += target
                    }
                }
        }

        return result
    }

    private fun detectCycles(
        itemIds: List<DynamicScheduleItemId>,
        dependencies: List<DynamicScheduleDependency>,
    ): List<List<DynamicScheduleItemId>> {
        val adjacency = itemIds.associateWith { mutableListOf<DynamicScheduleItemId>() }
        dependencies
            .filter { it.type != DynamicScheduleDependencyType.ASSOCIATED }
            .forEach { dependency ->
                adjacency.getValue(dependency.fromItemId).add(dependency.toItemId)
            }
        adjacency.values.forEach { it.sortBy(DynamicScheduleItemId::value) }

        val state = mutableMapOf<DynamicScheduleItemId, Int>()
        val stack = mutableListOf<DynamicScheduleItemId>()
        val cycles = mutableListOf<List<DynamicScheduleItemId>>()

        fun dfs(node: DynamicScheduleItemId) {
            state[node] = 1
            stack += node
            for (next in adjacency.getValue(node)) {
                when (state[next] ?: 0) {
                    0 -> dfs(next)
                    1 -> {
                        val index = stack.indexOf(next)
                        if (index >= 0) {
                            cycles += (stack.subList(index, stack.size) + next)
                        }
                    }
                }
            }
            stack.removeAt(stack.lastIndex)
            state[node] = 2
        }

        itemIds.sortedBy { it.value }.forEach { id ->
            if ((state[id] ?: 0) == 0) dfs(id)
        }
        return cycles
    }
}
