package me.moiclathy.dism.external.schedule.sync.journal

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Sync Journal 中的一条不可变事实记录。
 */
data class ExternalScheduleSyncJournalEvent(

    val journalId:
        ExternalScheduleSyncJournalId,

    val scheduleId:
        ExternalScheduleId,

    val runId:
        ExternalScheduleSyncRunId,

    val type:
        ExternalScheduleSyncJournalEventType,

    val occurredAt:
        Instant,

    val externalKey:
        String? = null,

    val action:
        ExternalScheduleSyncAction? = null,

    val eventId:
        String? = null,

    val message:
        String? = null,
) {

    init {

        if (
            externalKey != null
        ) {
            require(
                externalKey.isNotBlank()
            ) {
                "externalKey must not be blank"
            }
        }

        if (
            eventId != null
        ) {
            require(
                eventId.isNotBlank()
            ) {
                "eventId must not be blank"
            }
        }

        if (
            type ==
                ExternalScheduleSyncJournalEventType
                    .ACTION_STARTED ||
            type ==
                ExternalScheduleSyncJournalEventType
                    .ACTION_COMPLETED ||
            type ==
                ExternalScheduleSyncJournalEventType
                    .ACTION_FAILED ||
            type ==
                ExternalScheduleSyncJournalEventType
                    .ACTION_SKIPPED
        ) {
            require(
                externalKey != null
            ) {
                "Action journal event requires externalKey"
            }

            require(
                action != null
            ) {
                "Action journal event requires action"
            }
        }

        if (
            type ==
                ExternalScheduleSyncJournalEventType
                    .EVENT_CREATED ||
            type ==
                ExternalScheduleSyncJournalEventType
                    .EVENT_UPDATED ||
            type ==
                ExternalScheduleSyncJournalEventType
                    .MAPPING_COMMITTED
        ) {
            require(
                eventId != null
            ) {
                "Event journal event requires eventId"
            }
        }

        if (
            type ==
                ExternalScheduleSyncJournalEventType
                    .ACTION_FAILED
        ) {
            require(
                !message.isNullOrBlank()
            ) {
                "ACTION_FAILED requires message"
            }
        }
    }
}
