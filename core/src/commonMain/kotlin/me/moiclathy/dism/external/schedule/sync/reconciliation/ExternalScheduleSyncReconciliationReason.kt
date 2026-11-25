package me.moiclathy.dism.external.schedule.sync.reconciliation

/**
 * Reconciliation 决策原因。
 */
enum class ExternalScheduleSyncReconciliationReason {

    SYNC_ALREADY_COMPLETED,

    SYNC_IN_PROGRESS_EVENT_EXISTS,

    SYNC_IN_PROGRESS_EVENT_MISSING,

    SYNC_FAILED_EVENT_EXISTS,

    SYNC_FAILED_EVENT_MISSING,

    MISSING_EVENT_MAPPING,

    INACTIVE_EXTERNAL_RECORD,

    UNKNOWN_EVENT_MAPPING,

    NO_SYNC_RECORD,
}
