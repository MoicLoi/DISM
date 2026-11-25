package me.moiclathy.dism.external.schedule.sync.e2e

/**
 * Result of the 11.31 execution-level E2E boundary.
 */
data class ExternalScheduleSyncE2EResult(
    val status: ExternalScheduleSyncE2EStatus,
    val trace: ExternalScheduleSyncE2ETrace?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val successful: Boolean
        get() = status == ExternalScheduleSyncE2EStatus.COMPLETED ||
            status == ExternalScheduleSyncE2EStatus.NO_ACTIONS

    val hasActions: Boolean
        get() = trace?.actionCount?.let { it > 0 } == true

    val executedCount: Int
        get() = trace?.executedCount ?: 0

    val skippedCount: Int
        get() = trace?.skippedCount ?: 0

    val failedCount: Int
        get() = trace?.failedCount ?: errors.size
}
