package me.moiclathy.dism.external.schedule.sync.recovery

/**
 * Recovery 决策原因。
 */
enum class ExternalScheduleSyncRecoveryReason {

    FAILED_CREATE,

    FAILED_UPDATE,

    FAILED_REMOVAL,

    MISSING_EVENT_MAPPING,

    INACTIVE_EXTERNAL_RECORD,

    SUCCESSFUL_PREVIOUS_SYNC,

    ALREADY_RECONCILED,

    NO_SYNC_HISTORY,

    INVALID_SYNC_RECORD,
}
