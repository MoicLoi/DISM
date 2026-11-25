package me.moiclathy.dism.external.schedule.sync.journal

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * 一个 Sync Run 的完整 Journal Snapshot。
 */
data class ExternalScheduleSyncJournalSnapshot(

    val scheduleId:
        ExternalScheduleId,

    val runId:
        ExternalScheduleSyncRunId,

    val events:
        List<ExternalScheduleSyncJournalEvent>,
) {

    val orderedEvents:
        List<ExternalScheduleSyncJournalEvent>
        get() =
            events.sortedWith(
                compareBy<
                    ExternalScheduleSyncJournalEvent
                > {
                    it.occurredAt
                }.thenBy {
                    it.journalId.value
                }
            )

    val checkpoints:
        List<ExternalScheduleSyncCheckpoint>
        get() =
            buildCheckpoints()

    val completedCount: Int
        get() =
            checkpoints.count {
                it.status ==
                    ExternalScheduleSyncCheckpointStatus
                        .COMPLETED
            }

    val failedCount: Int
        get() =
            checkpoints.count {
                it.status ==
                    ExternalScheduleSyncCheckpointStatus
                        .FAILED
            }

    val pendingCount: Int
        get() =
            checkpoints.count {
                !it.terminal
            }

    private fun buildCheckpoints():
        List<ExternalScheduleSyncCheckpoint> {

        val grouped =
            orderedEvents
                .filter {
                    it.externalKey != null &&
                        it.action != null
                }
                .groupBy {
                    it.externalKey!! to
                        it.action!!
                }

        return grouped
            .map { (key, _) ->

                val externalKey =
                    key.first

                val action =
                    key.second

                val events =
                    actionEventList(
                        externalKey,
                        action
                    )

                projectCheckpoint(
                    externalKey,
                    action,
                    events,
                )
            }
            .sortedBy {
                it.externalKey
            }
    }

    private fun actionEventList(
        externalKey:
            String,

        action:
            me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction,
    ):
        List<ExternalScheduleSyncJournalEvent> {

        return orderedEvents.filter {
            it.externalKey == externalKey &&
                it.action == action
        }
    }

    private fun projectCheckpoint(
        externalKey:
            String,

        action:
            me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction,

        events:
            List<ExternalScheduleSyncJournalEvent>,
    ):
        ExternalScheduleSyncCheckpoint {

        val latest =
            events.lastOrNull()

        val started =
            events.firstOrNull {
                it.type ==
                    ExternalScheduleSyncJournalEventType
                        .ACTION_STARTED
            }

        val completed =
            events.lastOrNull {
                it.type ==
                    ExternalScheduleSyncJournalEventType
                        .ACTION_COMPLETED
            }

        val failed =
            events.lastOrNull {
                it.type ==
                    ExternalScheduleSyncJournalEventType
                        .ACTION_FAILED
            }

        val skipped =
            events.lastOrNull {
                it.type ==
                    ExternalScheduleSyncJournalEventType
                        .ACTION_SKIPPED
            }

        val mapping =
            events.lastOrNull {
                it.type ==
                    ExternalScheduleSyncJournalEventType
                        .MAPPING_COMMITTED
            }

        val created =
            events.lastOrNull {
                it.type ==
                    ExternalScheduleSyncJournalEventType
                        .EVENT_CREATED
            }

        val updated =
            events.lastOrNull {
                it.type ==
                    ExternalScheduleSyncJournalEventType
                        .EVENT_UPDATED
            }

        val status =
            when {

                completed != null ->
                    ExternalScheduleSyncCheckpointStatus
                        .COMPLETED

                failed != null ->
                    ExternalScheduleSyncCheckpointStatus
                        .FAILED

                skipped != null ->
                    ExternalScheduleSyncCheckpointStatus
                        .SKIPPED

                mapping != null ->
                    ExternalScheduleSyncCheckpointStatus
                        .MAPPING_COMMITTED

                created != null ->
                    ExternalScheduleSyncCheckpointStatus
                        .EVENT_CREATED

                updated != null ->
                    ExternalScheduleSyncCheckpointStatus
                        .EVENT_UPDATED

                started != null ->
                    ExternalScheduleSyncCheckpointStatus
                        .STARTED

                else ->
                    ExternalScheduleSyncCheckpointStatus
                        .NOT_STARTED
            }

        val eventId =
            listOf(
                completed?.eventId,
                mapping?.eventId,
                created?.eventId,
                updated?.eventId,
            )
                .firstOrNull {
                    !it.isNullOrBlank()
                }

        return ExternalScheduleSyncCheckpoint(

            externalKey =
                externalKey,

            action =
                action,

            status =
                status,

            eventId =
                eventId,

            startedAt =
                started?.occurredAt,

            lastUpdatedAt =
                latest?.occurredAt,

            message =
                failed?.message,
        )
    }
}
