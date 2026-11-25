package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalSnapshot
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRepository
import me.moiclathy.dism.external.schedule.sync.projection.DeterministicExternalScheduleSyncRunProjector
import me.moiclathy.dism.external.schedule.sync.projection.ExternalScheduleSyncRunProjection
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

class ExternalScheduleSyncJournalRecoveryBootstrap(
    private val persistenceRepository: ExternalScheduleSyncJournalPersistenceRepository,
    private val projector: DeterministicExternalScheduleSyncRunProjector,
) : ExternalScheduleSyncJournalRecoveryService {

    override fun recover(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncJournalRecoveryResult {
        val persisted = persistenceRepository.findByRun(runId)
            .filter { it.scheduleId == scheduleId }
            .sortedWith(compareBy<ExternalScheduleSyncJournalPersistenceRecord> { it.occurredAt }.thenBy { it.journalId.value })

        if (persisted.isEmpty()) {
            return ExternalScheduleSyncJournalRecoveryResult(
                scheduleId = scheduleId,
                runId = runId,
                status = ExternalScheduleSyncJournalRecoveryStatus.EMPTY,
                snapshot = null,
                projection = null,
                restoredEventCount = 0,
            )
        }

        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()
        val events = mutableListOf<ExternalScheduleSyncJournalEvent>()

        persisted.forEach { record ->
            runCatching { record.toDomain() }
                .onSuccess(events::add)
                .onFailure { throwable ->
                    errors += "Failed to restore journal ${record.journalId.value}: ${throwable.message ?: throwable::class.simpleName}"
                }
        }

        if (events.isEmpty()) {
            return ExternalScheduleSyncJournalRecoveryResult(
                scheduleId = scheduleId,
                runId = runId,
                status = ExternalScheduleSyncJournalRecoveryStatus.FAILED,
                snapshot = null,
                projection = null,
                restoredEventCount = 0,
                errors = errors.ifEmpty { listOf("No valid journal events could be restored") },
            )
        }

        val snapshot = ExternalScheduleSyncJournalSnapshot(
            scheduleId = scheduleId,
            runId = runId,
            events = events,
        )
        val projectionResult = projector.project(seed, snapshot, projectedAt)

        if (!projectionResult.successful || projectionResult.projection == null) {
            return ExternalScheduleSyncJournalRecoveryResult(
                scheduleId = scheduleId,
                runId = runId,
                status = ExternalScheduleSyncJournalRecoveryStatus.FAILED,
                snapshot = snapshot,
                projection = projectionResult.projection,
                restoredEventCount = events.size,
                warnings = projectionResult.warnings,
                errors = errors + projectionResult.errors,
            )
        }

        if (persisted.size != events.size) {
            warnings += "${persisted.size - events.size} persisted journal event(s) could not be restored"
        }

        return ExternalScheduleSyncJournalRecoveryResult(
            scheduleId = scheduleId,
            runId = runId,
            status = if (errors.isEmpty()) ExternalScheduleSyncJournalRecoveryStatus.RESTORED
            else ExternalScheduleSyncJournalRecoveryStatus.PARTIAL,
            snapshot = snapshot,
            projection = projectionResult.projection,
            restoredEventCount = events.size,
            warnings = warnings + projectionResult.warnings,
            errors = errors + projectionResult.errors,
        )
    }

    private fun ExternalScheduleSyncJournalPersistenceRecord.toDomain() = ExternalScheduleSyncJournalEvent(
        journalId = ExternalScheduleSyncJournalId(journalId.value),
        scheduleId = scheduleId,
        runId = runId,
        type = type,
        occurredAt = occurredAt,
        externalKey = externalKey,
        action = action,
        eventId = eventId,
        message = message,
    )
}
