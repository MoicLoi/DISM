package me.moiclathy.dism.external.schedule.sync.journal

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Sync Journal Service。
 */
interface ExternalScheduleSyncJournalService {

    fun recordRunStarted(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent

    fun recordActionStarted(
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
        ExternalScheduleSyncJournalEvent

    fun recordEventCreated(
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
        ExternalScheduleSyncJournalEvent

    fun recordEventUpdated(
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
        ExternalScheduleSyncJournalEvent

    fun recordMappingCommitted(
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
        ExternalScheduleSyncJournalEvent

    fun recordActionCompleted(
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
        ExternalScheduleSyncJournalEvent

    fun recordActionFailed(
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
        ExternalScheduleSyncJournalEvent

    fun recordActionSkipped(
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
        ExternalScheduleSyncJournalEvent

    fun recordRunCompleted(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        occurredAt:
            Instant,
    ):
        ExternalScheduleSyncJournalEvent

    fun snapshot(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,
    ):
        ExternalScheduleSyncJournalSnapshot
}
