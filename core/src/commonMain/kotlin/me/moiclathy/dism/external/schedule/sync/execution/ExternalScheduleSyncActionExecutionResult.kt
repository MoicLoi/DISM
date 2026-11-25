package me.moiclathy.dism.external.schedule.sync.execution

import me.moiclathy.dism.external.schedule.sync.idempotency.ExternalScheduleSyncActionExecutionRecord

data class ExternalScheduleSyncActionExecutionResult(
    val status: ExternalScheduleSyncActionExecutionStatus,
    val externalKey: String,
    val action: String,
    val eventId: String? = null,
    val idempotencyRecord: ExternalScheduleSyncActionExecutionRecord? = null,
    val message: String? = null,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val successful: Boolean
        get() = status != ExternalScheduleSyncActionExecutionStatus.FAILED &&
            status != ExternalScheduleSyncActionExecutionStatus.REJECTED &&
            errors.isEmpty()

    val executed: Boolean
        get() = status == ExternalScheduleSyncActionExecutionStatus.EXECUTED

    val skipped: Boolean
        get() = status == ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_IN_PROGRESS ||
            status == ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_COMPLETED
}
