package me.moiclathy.dism.external.schedule.event

/**
 * Event Factory 的执行结果。
 *
 * T 是最终的 DISM Event 类型。
 */
data class ExternalScheduleEventFactoryResult<T>(
    val event: T?,
    val externalKey: String?,
    val warnings: List<String> = emptyList(),
    val errors: List<ExternalScheduleEventFactoryError> = emptyList(),
) {

    val successful: Boolean
        get() =
            event != null && errors.isEmpty()

    val hasWarnings: Boolean
        get() =
            warnings.isNotEmpty()

    companion object {

        fun <T> success(
            event: T,
            externalKey: String,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleEventFactoryResult<T> =
            ExternalScheduleEventFactoryResult(
                event = event,
                externalKey = externalKey,
                warnings = warnings,
                errors = emptyList(),
            )

        fun <T> failure(
            errors: List<ExternalScheduleEventFactoryError>,
            externalKey: String? = null,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleEventFactoryResult<T> =
            ExternalScheduleEventFactoryResult(
                event = null,
                externalKey = externalKey,
                warnings = warnings,
                errors = errors,
            )
    }
}
