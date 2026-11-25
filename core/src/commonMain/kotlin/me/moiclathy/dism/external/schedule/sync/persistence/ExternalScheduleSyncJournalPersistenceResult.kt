package me.moiclathy.dism.external.schedule.sync.persistence

/** Journal 持久化操作结果。 */
data class ExternalScheduleSyncJournalPersistenceResult(
    val successful: Boolean,
    val affectedRecords: Int = 0,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    init {
        require(affectedRecords >= 0) { "affectedRecords must not be negative" }
    }

    companion object {
        fun success(affectedRecords: Int, warnings: List<String> = emptyList()) =
            ExternalScheduleSyncJournalPersistenceResult(
                successful = true,
                affectedRecords = affectedRecords,
                warnings = warnings,
            )

        fun failure(errors: List<String>, warnings: List<String> = emptyList()) =
            ExternalScheduleSyncJournalPersistenceResult(
                successful = false,
                warnings = warnings,
                errors = errors,
            )
    }
}
