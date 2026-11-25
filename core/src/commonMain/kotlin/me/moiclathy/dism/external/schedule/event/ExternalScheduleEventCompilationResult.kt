package me.moiclathy.dism.external.schedule.event

/**
 * 单个 ExternalScheduleEntry 的 Event 编译结果。
 */
data class ExternalScheduleEventCompilationResult(
    val drafts: List<ExternalScheduleEventDraft>,
    val errors: List<ExternalScheduleEventCompilationError>,
    val warnings: List<String> = emptyList(),
) {

    val successful: Boolean
        get() = errors.isEmpty()

    val hasWarnings: Boolean
        get() = warnings.isNotEmpty()

    companion object {

        fun success(
            drafts: List<ExternalScheduleEventDraft>,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleEventCompilationResult {
            return ExternalScheduleEventCompilationResult(
                drafts = drafts,
                errors = emptyList(),
                warnings = warnings,
            )
        }

        fun failure(
            errors: List<ExternalScheduleEventCompilationError>,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleEventCompilationResult {
            return ExternalScheduleEventCompilationResult(
                drafts = emptyList(),
                errors = errors,
                warnings = warnings,
            )
        }
    }
}
