package me.moiclathy.dism.external.schedule.sync.idempotency

import kotlin.time.Instant

/**
 * 一次 Sync Action 的幂等状态记录。
 */
data class ExternalScheduleSyncIdempotencyRecord(

    val key:
        ExternalScheduleSyncIdempotencyKey,

    val status:
        ExternalScheduleSyncIdempotencyStatus,

    val startedAt:
        Instant? = null,

    val completedAt:
        Instant? = null,

    val eventId:
        String? = null,

    val message:
        String? = null,
) {

    init {

        if (
            status ==
                ExternalScheduleSyncIdempotencyStatus
                    .COMPLETED
        ) {

            require(
                completedAt != null
            ) {
                "completedAt is required for COMPLETED"
            }
        }

        if (
            status ==
                ExternalScheduleSyncIdempotencyStatus
                    .FAILED
        ) {

            require(
                !message.isNullOrBlank()
            ) {
                "message is required for FAILED"
            }
        }

        if (
            status ==
                ExternalScheduleSyncIdempotencyStatus
                    .IN_PROGRESS
        ) {

            require(
                startedAt != null
            ) {
                "startedAt is required for IN_PROGRESS"
            }
        }
    }

    val terminal: Boolean
        get() =
            status ==
                ExternalScheduleSyncIdempotencyStatus
                    .COMPLETED ||
            status ==
                ExternalScheduleSyncIdempotencyStatus
                    .FAILED
}
