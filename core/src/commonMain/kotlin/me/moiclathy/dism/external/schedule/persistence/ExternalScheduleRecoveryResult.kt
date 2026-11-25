package me.moiclathy.dism.external.schedule.persistence

/**
 * Recovery 操作结果。
 */
data class ExternalScheduleRecoveryResult(
    val snapshot:
        ExternalScheduleRecoverySnapshot?,

    val warnings: List<String> = emptyList(),

    val errors: List<String> = emptyList(),
) {

    val successful: Boolean
        get() =
            snapshot != null &&
                errors.isEmpty()

    val restoredCount: Int
        get() =
            snapshot
                ?.restoredRecords
                ?.size
                ?: 0

    val inactiveCount: Int
        get() =
            snapshot
                ?.inactiveRecords
                ?.size
                ?: 0

    val missingEventMappingCount: Int
        get() =
            snapshot
                ?.missingEventMappings
                ?.size
                ?: 0

    val invalidCount: Int
        get() =
            snapshot
                ?.invalidRecords
                ?.size
                ?: 0

    companion object {

        fun success(
            snapshot: ExternalScheduleRecoverySnapshot,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleRecoveryResult =
            ExternalScheduleRecoveryResult(
                snapshot = snapshot,
                warnings = warnings,
            )

        fun failure(
            errors: List<String>,
            warnings: List<String> = emptyList(),
        ): ExternalScheduleRecoveryResult =
            ExternalScheduleRecoveryResult(
                snapshot = null,
                warnings = warnings,
                errors = errors,
            )
    }
}
