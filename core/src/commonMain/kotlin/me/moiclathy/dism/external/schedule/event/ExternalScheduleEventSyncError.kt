package me.moiclathy.dism.external.schedule.event

/**
 * Event Sync 错误类型。
 */
enum class ExternalScheduleEventSyncError {

    INVALID_EXTERNAL_KEY,

    MISSING_CURRENT_RECORD,

    MISSING_PREVIOUS_RECORD,

    MISSING_EVENT_MAPPING,

    CREATE_FAILED,

    UPDATE_FAILED,

    REMOVAL_FAILED,

    EVENT_ID_NOT_PRESERVED,

    UNSUPPORTED_ACTION,

    INTERNAL_ERROR,
}
