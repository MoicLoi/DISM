package me.moiclathy.dism.external.schedule.normalization

/**
 * 规范化结果。
 */
data class ExternalScheduleNormalizationResult(
    val schedule: NormalizedExternalSchedule?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {

    init {
        require(
            warnings.all {
                it.isNotBlank()
            }
        ) {
            "Normalization warnings must not be blank."
        }

        require(
            errors.all {
                it.isNotBlank()
            }
        ) {
            "Normalization errors must not be blank."
        }

        if (schedule == null) {
            require(errors.isNotEmpty()) {
                "Failed normalization must contain errors."
            }
        }
    }

    val success: Boolean
        get() =
            schedule != null &&
                errors.isEmpty()

    val hasWarnings: Boolean
        get() =
            warnings.isNotEmpty()

    companion object {

        fun success(
            schedule: NormalizedExternalSchedule,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleNormalizationResult =
            ExternalScheduleNormalizationResult(
                schedule = schedule,
                warnings = warnings,
                errors = emptyList(),
            )

        fun failure(
            error: String,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleNormalizationResult {

            require(error.isNotBlank())

            return ExternalScheduleNormalizationResult(
                schedule = null,
                warnings = warnings,
                errors = listOf(error),
            )
        }
    }
}
