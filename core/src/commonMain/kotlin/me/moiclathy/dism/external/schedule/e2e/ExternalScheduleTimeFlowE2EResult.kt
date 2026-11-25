package me.moiclathy.dism.external.schedule.e2e

data class ExternalScheduleTimeFlowE2EResult(
    val trace: ExternalScheduleTimeFlowE2ETrace?,
    val contractErrors: List<String> = emptyList(),
) {

    val successful: Boolean
        get() =
            trace != null &&
                contractErrors.isEmpty() &&
                trace.successful

    companion object {

        fun success(
            trace: ExternalScheduleTimeFlowE2ETrace,
        ): ExternalScheduleTimeFlowE2EResult {

            val errors =
                ExternalScheduleTimeFlowE2EContract
                    .validate(trace)

            return ExternalScheduleTimeFlowE2EResult(
                trace = trace,
                contractErrors = errors,
            )
        }

        fun failure(
            trace: ExternalScheduleTimeFlowE2ETrace,
            errors: List<String>,
        ): ExternalScheduleTimeFlowE2EResult =
            ExternalScheduleTimeFlowE2EResult(
                trace = trace,
                contractErrors = errors,
            )
    }
}
