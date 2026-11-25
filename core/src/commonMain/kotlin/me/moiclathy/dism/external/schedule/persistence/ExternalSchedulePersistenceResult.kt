package me.moiclathy.dism.external.schedule.persistence

/**
 * Persistence 操作结果。
 */
data class ExternalSchedulePersistenceResult(
    val successful: Boolean,

    val affectedRecords: Int = 0,

    val warnings: List<String> = emptyList(),

    val errors: List<String> = emptyList(),
) {

    init {
        require(warnings.all { it.isNotBlank() }) {
            "warnings must not contain blank values"
        }

        require(errors.all { it.isNotBlank() }) {
            "errors must not contain blank values"
        }

        require(affectedRecords >= 0) {
            "affectedRecords must not be negative"
        }
    }

    companion object {

        fun success(
            affectedRecords: Int = 0,
            warnings: List<String> = emptyList(),
        ): ExternalSchedulePersistenceResult =
            ExternalSchedulePersistenceResult(
                successful = true,
                affectedRecords = affectedRecords,
                warnings = warnings,
            )

        fun failure(
            errors: List<String>,
            warnings: List<String> = emptyList(),
        ): ExternalSchedulePersistenceResult =
            ExternalSchedulePersistenceResult(
                successful = false,
                warnings = warnings,
                errors = errors,
            )
    }
}
