package me.moiclathy.dism.external.schedule.sync.journal

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * 确定性的 Journal Service。
 */
@OptIn(ExperimentalUuidApi::class)
class DeterministicExternalScheduleSyncJournalService(
    private val repository:
        ExternalScheduleSyncJournalRepository,
) : ExternalScheduleSyncJournalService {

    override fun recordRunStarted(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .RUN_STARTED,

            occurredAt =
                occurredAt,
        )
    }

    override fun recordActionStarted(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        externalKey:
            String,

        action:
            ExternalScheduleSyncAction,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .ACTION_STARTED,

            occurredAt =
                occurredAt,

            externalKey =
                externalKey,

            action =
                action,
        )
    }

    override fun recordEventCreated(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        externalKey:
            String,

        action:
            ExternalScheduleSyncAction,

        eventId:
            String,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .EVENT_CREATED,

            occurredAt =
                occurredAt,

            externalKey =
                externalKey,

            action =
                action,

            eventId =
                eventId,
        )
    }

    override fun recordEventUpdated(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        externalKey:
            String,

        action:
            ExternalScheduleSyncAction,

        eventId:
            String,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .EVENT_UPDATED,

            occurredAt =
                occurredAt,

            externalKey =
                externalKey,

            action =
                action,

            eventId =
                eventId,
        )
    }

    override fun recordMappingCommitted(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        externalKey:
            String,

        action:
            ExternalScheduleSyncAction,

        eventId:
            String,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .MAPPING_COMMITTED,

            occurredAt =
                occurredAt,

            externalKey =
                externalKey,

            action =
                action,

            eventId =
                eventId,
        )
    }

    override fun recordActionCompleted(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        externalKey:
            String,

        action:
            ExternalScheduleSyncAction,

        eventId:
            String?,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .ACTION_COMPLETED,

            occurredAt =
                occurredAt,

            externalKey =
                externalKey,

            action =
                action,

            eventId =
                eventId,
        )
    }

    override fun recordActionFailed(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        externalKey:
            String,

        action:
            ExternalScheduleSyncAction,

        message:
            String,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        require(message.isNotBlank()) {
            "message must not be blank"
        }

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .ACTION_FAILED,

            occurredAt =
                occurredAt,

            externalKey =
                externalKey,

            action =
                action,

            message =
                message,
        )
    }

    override fun recordActionSkipped(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        externalKey:
            String,

        action:
            ExternalScheduleSyncAction,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .ACTION_SKIPPED,

            occurredAt =
                occurredAt,

            externalKey =
                externalKey,

            action =
                action,
        )
    }

    override fun recordRunCompleted(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent {

        return append(
            scheduleId =
                scheduleId,

            runId =
                runId,

            type =
                ExternalScheduleSyncJournalEventType
                    .RUN_COMPLETED,

            occurredAt =
                occurredAt,
        )
    }

    override fun snapshot(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,
    ):
        ExternalScheduleSyncJournalSnapshot {

        return repository.snapshot(
            scheduleId,
            runId,
        )
    }

    private fun append(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        type:
            ExternalScheduleSyncJournalEventType,

        occurredAt:
            Instant,

        externalKey:
            String? = null,

        action:
            ExternalScheduleSyncAction? = null,

        eventId:
            String? = null,

        message:
            String? = null,
    ):
        ExternalScheduleSyncJournalEvent {

        /*
         * Journal ID 采用真正的随机 Uuid。
         *
         * scheduleId + runId + occurredAt + externalKey
         * 只作为业务排序 / 查询字段，
         * 避免并发写入同一时间戳时发生 ID 碰撞。
         */
        val journalId =
            ExternalScheduleSyncJournalId(
                Uuid.random().toString()
            )

        val event =
            ExternalScheduleSyncJournalEvent(

                journalId =
                    journalId,

                scheduleId =
                    scheduleId,

                runId =
                    runId,

                type =
                    type,

                occurredAt =
                    occurredAt,

                externalKey =
                    externalKey,

                action =
                    action,

                eventId =
                    eventId,

                message =
                    message,
            )

        return repository.append(
            event
        )
    }

}
