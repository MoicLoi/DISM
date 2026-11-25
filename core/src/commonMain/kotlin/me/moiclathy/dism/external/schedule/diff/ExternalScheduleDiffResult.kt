package me.moiclathy.dism.external.schedule.diff

data class ExternalScheduleDiffResult(
    val diff: ExternalScheduleDiff?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {

    val successful: Boolean
        get() =
            diff != null &&
                errors.isEmpty()

    companion object {

        fun success(
            diff: ExternalScheduleDiff,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleDiffResult =
            ExternalScheduleDiffResult(
                diff = diff,
                warnings = warnings,
            )

        fun failure(
            errors: List<String>,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleDiffResult =
            ExternalScheduleDiffResult(
                diff = null,
                warnings = warnings,
                errors = errors,
            )
    }
}
