package me.moiclathy.dism.external.schedule.event

/**
 * 单次 Event Sync 操作结果。
 */
data class ExternalScheduleEventSyncResult(
    val successful: Boolean,

    val action:
        String,

    val externalKey:
        String,

    val eventId:
        String? = null,

    val error:
        ExternalScheduleEventSyncError? = null,

    val message:
        String? = null,
) {

    companion object {

        fun success(
            action: String,
            externalKey: String,
            eventId: String?,
        ): ExternalScheduleEventSyncResult =
            ExternalScheduleEventSyncResult(
                successful = true,
                action = action,
                externalKey = externalKey,
                eventId = eventId,
            )

        fun failure(
            action: String,
            externalKey: String,
            error:
                ExternalScheduleEventSyncError,
            message: String,
            eventId: String? = null,
        ): ExternalScheduleEventSyncResult =
            ExternalScheduleEventSyncResult(
                successful = false,
                action = action,
                externalKey = externalKey,
                eventId = eventId,
                error = error,
                message = message,
            )
    }
}
