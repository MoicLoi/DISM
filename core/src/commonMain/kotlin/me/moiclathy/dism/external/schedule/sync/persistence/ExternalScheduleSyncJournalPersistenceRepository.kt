package me.moiclathy.dism.external.schedule.sync.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Journal 持久化边界。
 *
 * 实际 SQLDelight 适配器只需要实现本接口。
 */
interface ExternalScheduleSyncJournalPersistenceRepository {
    fun append(record: ExternalScheduleSyncJournalPersistenceRecord): ExternalScheduleSyncJournalPersistenceRecord
    fun appendAll(records: List<ExternalScheduleSyncJournalPersistenceRecord>): List<ExternalScheduleSyncJournalPersistenceRecord>
    fun find(journalId: ExternalScheduleSyncJournalId): ExternalScheduleSyncJournalPersistenceRecord?
    fun findByRun(runId: ExternalScheduleSyncRunId): List<ExternalScheduleSyncJournalPersistenceRecord>
    fun findBySchedule(scheduleId: ExternalScheduleId): List<ExternalScheduleSyncJournalPersistenceRecord>
    fun snapshot(scheduleId: ExternalScheduleId, runId: ExternalScheduleSyncRunId): ExternalScheduleSyncJournalPersistenceSnapshot
    fun deleteRun(runId: ExternalScheduleSyncRunId): Int
}
