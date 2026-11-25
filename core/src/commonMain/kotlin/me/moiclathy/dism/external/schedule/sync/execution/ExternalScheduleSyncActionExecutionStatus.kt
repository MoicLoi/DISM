package me.moiclathy.dism.external.schedule.sync.execution

enum class ExternalScheduleSyncActionExecutionStatus {
    EXECUTED,
    SKIPPED_ALREADY_IN_PROGRESS,
    SKIPPED_ALREADY_COMPLETED,
    FAILED,
    REJECTED,
}
