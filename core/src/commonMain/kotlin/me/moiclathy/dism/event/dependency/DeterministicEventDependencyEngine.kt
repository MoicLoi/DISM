package me.moiclathy.dism.event.dependency

/**
 * 确定性的 Event Dependency Engine。
 *
 * 所有结果只由：
 *
 * input graph + dependency + policy
 *
 * 决定。
 *
 * 不读取外部状态。
 * 不修改输入 Graph。
 */
class DeterministicEventDependencyEngine(
    private val policy: EventDependencyPolicy =
        EventDependencyPolicy(),
) : EventDependencyEngine {

    override fun addDependency(
        graph: EventDependencyGraph,
        dependency: EventDependency,
    ): EventDependencyValidationResult {

        /*
         * 重复依赖。
         */
        if (
            graph.contains(dependency) &&
            !policy.allowDuplicateDependencies
        ) {
            return EventDependencyValidationResult(
                valid = false,
                graph = graph,
                errors =
                    listOf(
                        "Duplicate dependency."
                    ),
            )
        }

        val newGraph =
            graph.add(dependency)

        return validate(newGraph)
    }

    override fun removeDependency(
        graph: EventDependencyGraph,
        dependency: EventDependency,
    ): EventDependencyGraph =
        graph.remove(dependency)

    override fun validate(
        graph: EventDependencyGraph,
    ): EventDependencyValidationResult {

        val errors =
            mutableListOf<String>()

        /*
         * 检查重复 Dependency。
         */
        if (!policy.allowDuplicateDependencies) {

            val duplicateCount =
                graph.dependencies
                    .groupingBy {
                        it
                    }
                    .eachCount()
                    .filterValues {
                        it > 1
                    }

            if (duplicateCount.isNotEmpty()) {
                errors +=
                    "Duplicate dependencies detected."
            }
        }

        /*
         * ASSOCIATED 默认不参与方向性循环检测。
         */
        val orderingDependencies =
            graph.dependencies.filter {
                it.type !=
                    EventDependencyType.ASSOCIATED ||
                    policy.associatedParticipatesInOrdering
            }

        /*
         * 检查环。
         */
        if (!policy.allowCycles) {

            val cycle =
                findCycle(
                    orderingDependencies,
                )

            if (cycle != null) {
                errors +=
                    "Dependency cycle detected: " +
                        cycle.joinToString(
                            separator = " -> "
                        )
            }
        }

        return EventDependencyValidationResult(
            valid =
                errors.isEmpty(),

            graph =
                graph,

            errors =
                errors,
        )
    }

    /**
     * DFS 检测有向图中的环。
     */
    private fun findCycle(
        dependencies: List<EventDependency>,
    ): List<String>? {

        val adjacency =
            dependencies
                .groupBy {
                    it.objectEventId
                }
                .mapValues { (_, values) ->
                    values.map {
                        it.subjectEventId
                    }
                }

        val visiting =
            mutableSetOf<String>()

        val visited =
            mutableSetOf<String>()

        val path =
            mutableListOf<String>()

        for (node in adjacency.keys) {

            val cycle =
                dfs(
                    node =
                        node,

                    adjacency =
                        adjacency,

                    visiting =
                        visiting,

                    visited =
                        visited,

                    path =
                        path,
                )

            if (cycle != null) {
                return cycle
            }
        }

        return null
    }

    private fun dfs(
        node: String,
        adjacency: Map<String, List<String>>,
        visiting: MutableSet<String>,
        visited: MutableSet<String>,
        path: MutableList<String>,
    ): List<String>? {

        if (node in visiting) {

            val index =
                path.indexOf(node)

            if (index >= 0) {
                return (
                    path.subList(
                        index,
                        path.size,
                    ) + node
                )
            }

            return listOf(
                node,
                node,
            )
        }

        if (node in visited) {
            return null
        }

        visiting += node
        path += node

        for (
            next in adjacency[node].orEmpty()
        ) {

            val cycle =
                dfs(
                    node =
                        next,

                    adjacency =
                        adjacency,

                    visiting =
                        visiting,

                    visited =
                        visited,

                    path =
                        path,
                )

            if (cycle != null) {
                return cycle
            }
        }

        path.removeAt(
            path.lastIndex
        )

        visiting -= node
        visited += node

        return null
    }
}
