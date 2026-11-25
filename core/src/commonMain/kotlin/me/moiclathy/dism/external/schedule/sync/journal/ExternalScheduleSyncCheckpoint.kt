package me.moiclathy.dism.external.schedule.sync.journal

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction

/**
 * 从 Journal Projection 出来的单个 Action Checkpoint。
 *
 * 它不是原始事实。
 *
 * 它是：
 *
 * Journal Events
 *      ↓
 * Checkpoint Projection
 */
data class ExternalScheduleSyncCheckpoint(

    val externalKey:
        String,

    val action:
        ExternalScheduleSyncAction,

    val status:
        ExternalScheduleSyncCheckpointStatus,

    val eventId:
        String? = null,

    val startedAt:
        Instant? = null,

    val lastUpdatedAt:
        Instant? = null,

    val message:
        String? = null,
) {

    init {

        require(externalKey.isNotBlank()) {
            "externalKey must not be blank"
        }

        if (
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .COMPLETED ||
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .EVENT_CREATED ||
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .EVENT_UPDATED ||
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .MAPPING_COMMITTED
        ) {

            require(
                !eventId.isNullOrBlank()
            ) {
                "eventId is required for successful Event checkpoint"
            }
        }

        if (
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .FAILED
        ) {

            require(
                !message.isNullOrBlank()
            ) {
                "message is required for failed checkpoint"
            }
        }
    }

    val terminal: Boolean
        get() =
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .COMPLETED ||
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .SKIPPED ||
            status ==
                ExternalScheduleSyncCheckpointStatus
                    .FAILED
}
