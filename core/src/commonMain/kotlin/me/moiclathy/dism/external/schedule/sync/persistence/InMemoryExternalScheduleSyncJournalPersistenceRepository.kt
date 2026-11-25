package me.moiclathy.dism.external.schedule.sync.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/** Journal 持久化的内存实现，主要用于测试与本地原型。 */
class InMemoryExternalScheduleSyncJournalPersistenceRepository : ExternalScheduleSyncJournalPersistenceRepository {
    private val records = linkedMapOf<String, ExternalScheduleSyncJournalPersistenceRecord>()

    override fun append(record: ExternalScheduleSyncJournalPersistenceRecord): ExternalScheduleSyncJournalPersistenceRecord {
        return records.getOrPut(record.journalId.value) { record }
    }

    override fun appendAll(records: List<ExternalScheduleSyncJournalPersistenceRecord>): List<ExternalScheduleSyncJournalPersistenceRecord> =
        records.map(::append)

    override fun find(journalId: ExternalScheduleSyncJournalId): ExternalScheduleSyncJournalPersistenceRecord? =
        records[journalId.value]

    override fun findByRun(runId: ExternalScheduleSyncRunId): List<ExternalScheduleSyncJournalPersistenceRecord> =
        records.values.filter { it.runId == runId }.sortedWith(compareBy<ExternalScheduleSyncJournalPersistenceRecord> { it.occurredAt }.thenBy { it.journalId.value })

    override fun findBySchedule(scheduleId: ExternalScheduleId): List<ExternalScheduleSyncJournalPersistenceRecord> =
        records.values.filter { it.scheduleId == scheduleId }.sortedWith(compareBy<ExternalScheduleSyncJournalPersistenceRecord> { it.occurredAt }.thenBy { it.journalId.value })

    override fun snapshot(scheduleId: ExternalScheduleId, runId: ExternalScheduleSyncRunId): ExternalScheduleSyncJournalPersistenceSnapshot =
        ExternalScheduleSyncJournalPersistenceSnapshot(
            scheduleId = scheduleId,
            runId = runId,
            records = findByRun(runId).filter { it.scheduleId == scheduleId },
        )

    override fun deleteRun(runId: ExternalScheduleSyncRunId): Int {
        val ids = records.values.filter { it.runId == runId }.map { it.journalId.value }
        ids.forEach(records::remove)
        return ids.size
    }
}
