package me.moiclathy.dism.external.schedule.sync.idempotency

/**
 * 幂等检查结果。
 */
data class ExternalScheduleSyncIdempotencyResult<T>(

    val successful:
        Boolean,

    val value:
        T? = null,

    val reusedExistingResult:
        Boolean = false,

    val warnings:
        List<String> = emptyList(),

    val errors:
        List<String> = emptyList(),
) {

    companion object {

        fun <T> success(
            value: T,
            reusedExistingResult: Boolean = false,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleSyncIdempotencyResult<T> =
            ExternalScheduleSyncIdempotencyResult(

                successful = true,

                value = value,

                reusedExistingResult =
                    reusedExistingResult,

                warnings =
                    warnings,
            )

        fun <T> failure(
            errors: List<String>,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleSyncIdempotencyResult<T> =
            ExternalScheduleSyncIdempotencyResult(

                successful = false,

                warnings =
                    warnings,

                errors =
                    errors,
            )
    }
}
