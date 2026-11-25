package me.moiclathy.dism.external.schedule.sync.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Domain Journal Event 与持久化边界之间的服务。
 */
class ExternalScheduleSyncJournalPersistenceService(
    private val repository: ExternalScheduleSyncJournalPersistenceRepository,
) {
    fun append(event: ExternalScheduleSyncJournalEvent): ExternalScheduleSyncJournalPersistenceResult {
        return try {
            repository.append(ExternalScheduleSyncJournalPersistenceRecord.fromDomain(event))
            ExternalScheduleSyncJournalPersistenceResult.success(1)
        } catch (exception: Exception) {
            ExternalScheduleSyncJournalPersistenceResult.failure(
                listOf(exception.message ?: "Failed to persist journal event")
            )
        }
    }

    fun appendAll(events: List<ExternalScheduleSyncJournalEvent>): ExternalScheduleSyncJournalPersistenceResult {
        if (events.isEmpty()) return ExternalScheduleSyncJournalPersistenceResult.success(0)
        return try {
            repository.appendAll(events.map(ExternalScheduleSyncJournalPersistenceRecord::fromDomain))
            ExternalScheduleSyncJournalPersistenceResult.success(events.size)
        } catch (exception: Exception) {
            ExternalScheduleSyncJournalPersistenceResult.failure(
                listOf(exception.message ?: "Failed to persist journal events")
            )
        }
    }

    fun restoreRun(runId: ExternalScheduleSyncRunId): List<ExternalScheduleSyncJournalEvent> =
        repository.findByRun(runId).map(ExternalScheduleSyncJournalPersistenceRecord::toDomain)

    fun restoreSnapshot(scheduleId: ExternalScheduleId, runId: ExternalScheduleSyncRunId) =
        repository.snapshot(scheduleId, runId).orderedRecords.map(ExternalScheduleSyncJournalPersistenceRecord::toDomain)

    fun find(eventId: ExternalScheduleSyncJournalId): ExternalScheduleSyncJournalEvent? =
        repository.find(eventId)?.toDomain()

    fun findByType(runId: ExternalScheduleSyncRunId, type: ExternalScheduleSyncJournalEventType): List<ExternalScheduleSyncJournalEvent> =
        restoreRun(runId).filter { it.type == type }

    fun deleteRun(runId: ExternalScheduleSyncRunId): ExternalScheduleSyncJournalPersistenceResult {
        return try {
            val affected = repository.deleteRun(runId)
            ExternalScheduleSyncJournalPersistenceResult.success(affected)
        } catch (exception: Exception) {
            ExternalScheduleSyncJournalPersistenceResult.failure(
                listOf(exception.message ?: "Failed to delete journal run")
            )
        }
    }
}
