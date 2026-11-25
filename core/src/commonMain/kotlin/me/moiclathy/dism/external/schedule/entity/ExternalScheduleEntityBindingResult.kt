package me.moiclathy.dism.external.schedule.entity

/**
 * 单个 External Schedule Entry 的 Entity Binding 结果。
 */
data class ExternalScheduleEntityBindingResult(
    val bindings: List<ExternalScheduleEntityBinding>,
    val candidates: List<ExternalScheduleEntityBindingCandidate> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {

    init {
        require(
            warnings.all { it.isNotBlank() }
        ) {
            "Binding warnings must not be blank."
        }

        require(
            errors.all { it.isNotBlank() }
        ) {
            "Binding errors must not be blank."
        }
    }

    val success: Boolean
        get() =
            errors.isEmpty()

    val hasBindings: Boolean
        get() =
            bindings.isNotEmpty()

    val hasWarnings: Boolean
        get() =
            warnings.isNotEmpty()
}
