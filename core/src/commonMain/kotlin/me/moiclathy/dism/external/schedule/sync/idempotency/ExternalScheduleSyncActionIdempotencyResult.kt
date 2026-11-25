package me.moiclathy.dism.external.schedule.sync.idempotency

enum class ExternalScheduleSyncActionIdempotencyDecision {
    STARTED,
    ALREADY_IN_PROGRESS,
    ALREADY_COMPLETED,
    RETRY_ALLOWED,
    REJECTED,
}

data class ExternalScheduleSyncActionIdempotencyResult(
    val decision: ExternalScheduleSyncActionIdempotencyDecision,
    val record: ExternalScheduleSyncActionExecutionRecord?,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val successful: Boolean
        get() = errors.isEmpty() &&
            decision != ExternalScheduleSyncActionIdempotencyDecision.REJECTED

    val shouldExecute: Boolean
        get() = decision == ExternalScheduleSyncActionIdempotencyDecision.STARTED ||
            decision == ExternalScheduleSyncActionIdempotencyDecision.RETRY_ALLOWED

    val terminal: Boolean
        get() = record?.terminal == true
}
