package me.moiclathy.dism.event.dependency

/**
 * Dependency Graph 验证结果。
 */
data class EventDependencyValidationResult(
    val valid: Boolean,
    val graph: EventDependencyGraph,
    val errors: List<String> = emptyList(),
) {

    init {
        require(valid || errors.isNotEmpty()) {
            "An invalid dependency graph must contain errors."
        }
    }
}
