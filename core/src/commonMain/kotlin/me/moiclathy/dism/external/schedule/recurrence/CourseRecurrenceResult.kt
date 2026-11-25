package me.moiclathy.dism.external.schedule.recurrence

/**
 * Recurrence Engine 的结果。
 */
data class CourseRecurrenceResult(
    val expansion: CourseRecurrenceExpansion?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {

    init {
        require(
            warnings.all { it.isNotBlank() }
        ) {
            "Recurrence warnings must not be blank."
        }

        require(
            errors.all { it.isNotBlank() }
        ) {
            "Recurrence errors must not be blank."
        }

        if (expansion == null) {
            require(errors.isNotEmpty()) {
                "Failed recurrence result must contain errors."
            }
        }
    }

    val success: Boolean
        get() =
            expansion != null &&
                errors.isEmpty()

    companion object {

        fun success(
            expansion: CourseRecurrenceExpansion,
            warnings: List<String> = emptyList(),
        ): CourseRecurrenceResult =
            CourseRecurrenceResult(
                expansion = expansion,
                warnings = warnings,
            )

        fun failure(
            error: String,
        ): CourseRecurrenceResult {

            require(error.isNotBlank())

            return CourseRecurrenceResult(
                expansion = null,
                errors = listOf(error),
            )
        }
    }
}
