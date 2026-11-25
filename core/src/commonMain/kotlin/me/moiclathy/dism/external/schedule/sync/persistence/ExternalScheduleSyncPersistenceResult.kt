package me.moiclathy.dism.external.schedule.sync.persistence

/**
 * Sync Persistence 操作结果。
 */
data class ExternalScheduleSyncPersistenceResult(

    val successful:
        Boolean,

    val affectedRecords:
        List<ExternalScheduleSyncPersistenceRecord> =
        emptyList(),

    val warnings:
        List<String> =
        emptyList(),

    val errors:
        List<String> =
        emptyList(),
) {

    val affectedCount: Int
        get() =
            affectedRecords.size

    companion object {

        fun success(
            records:
                List<ExternalScheduleSyncPersistenceRecord> =
                emptyList(),

            warnings:
                List<String> =
                emptyList(),
        ): ExternalScheduleSyncPersistenceResult =
            ExternalScheduleSyncPersistenceResult(
                successful = true,
                affectedRecords = records,
                warnings = warnings,
            )

        fun failure(
            errors:
                List<String>,

            records:
                List<ExternalScheduleSyncPersistenceRecord> =
                emptyList(),

            warnings:
                List<String> =
                emptyList(),
        ): ExternalScheduleSyncPersistenceResult =
            ExternalScheduleSyncPersistenceResult(
                successful = false,
                affectedRecords = records,
                warnings = warnings,
                errors = errors,
            )
    }
}
