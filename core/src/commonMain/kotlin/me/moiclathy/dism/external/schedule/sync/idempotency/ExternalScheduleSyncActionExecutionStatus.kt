package me.moiclathy.dism.external.schedule.sync.idempotency

enum class ExternalScheduleSyncActionExecutionStatus {
    NEW,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
}
