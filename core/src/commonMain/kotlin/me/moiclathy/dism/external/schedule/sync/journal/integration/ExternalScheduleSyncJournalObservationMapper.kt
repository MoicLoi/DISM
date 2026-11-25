package me.moiclathy.dism.external.schedule.sync.journal.integration

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.e2e.ExternalScheduleSyncE2EActionTrace
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

interface ExternalScheduleSyncJournalObservationMapper {
    fun mapRunStarted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
    ): ExternalScheduleSyncJournalEvent

    fun mapRecoveryStarted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
    ): ExternalScheduleSyncJournalEvent

    fun mapAction(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        trace: ExternalScheduleSyncE2EActionTrace,
        occurredAt: Instant,
        sequenceStart: Int,
    ): List<ExternalScheduleSyncJournalEvent>

    fun mapRecoveryCompleted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
        message: String?,
    ): ExternalScheduleSyncJournalEvent

    fun mapRunCompleted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
        message: String?,
    ): ExternalScheduleSyncJournalEvent
}
