package me.moiclathy.dism.external.schedule.parser

import me.moiclathy.dism.external.schedule.ExternalSchedule

/**
 * External Schedule Parser 结果。
 */
data class ExternalScheduleParseResult(
    val schedule: ExternalSchedule?,
    val errors: List<ExternalScheduleParseError> = emptyList(),
    val warnings: List<String> = emptyList(),
) {

    init {
        if (schedule != null) {
            require(errors.isEmpty()) {
                "Successful parse result must not contain errors."
            }
        } else {
            require(errors.isNotEmpty()) {
                "Failed parse result must contain at least one error."
            }
        }

        require(
            warnings.all {
                it.isNotBlank()
            }
        ) {
            "Parser warnings must not be blank."
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
            schedule: ExternalSchedule,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleParseResult {

            return ExternalScheduleParseResult(
                schedule =
                    schedule,

                errors =
                    emptyList(),

                warnings =
                    warnings,
            )
        }

        fun failure(
            errors: List<ExternalScheduleParseError>,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleParseResult {

            require(errors.isNotEmpty()) {
                "Parser failure must contain at least one error."
            }

            return ExternalScheduleParseResult(
                schedule =
                    null,

                errors =
                    errors,

                warnings =
                    warnings,
            )
        }

        fun failure(
            error: ExternalScheduleParseError,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleParseResult =
            failure(
                errors =
                    listOf(error),

                warnings =
                    warnings,
            )
    }
}
