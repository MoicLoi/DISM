package me.moiclathy.dism.external.schedule.sync.idempotency

import kotlin.time.Instant

data class ExternalScheduleSyncActionExecutionRecord(
    val identity: ExternalScheduleSyncActionIdentity,
    val status: ExternalScheduleSyncActionExecutionStatus,
    val eventId: String? = null,
    val startedAt: Instant? = null,
    val completedAt: Instant? = null,
    val updatedAt: Instant,
    val message: String? = null,
) {
    init {
        if (status == ExternalScheduleSyncActionExecutionStatus.COMPLETED) {
            require(completedAt != null) { "completed action must have completedAt" }
        }
        if (status == ExternalScheduleSyncActionExecutionStatus.FAILED) {
            require(!message.isNullOrBlank()) { "failed action must have message" }
        }
        if (startedAt != null && completedAt != null) {
            require(completedAt >= startedAt) {
                "completedAt must not be earlier than startedAt"
            }
        }
    }

    val terminal: Boolean
        get() = status == ExternalScheduleSyncActionExecutionStatus.COMPLETED ||
            status == ExternalScheduleSyncActionExecutionStatus.FAILED
}
