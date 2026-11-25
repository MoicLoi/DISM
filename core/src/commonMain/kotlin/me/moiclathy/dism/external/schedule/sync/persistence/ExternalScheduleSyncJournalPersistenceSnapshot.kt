package me.moiclathy.dism.external.schedule.sync.persistence

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/** 一次 Sync Run 的 Journal 持久化快照。 */
data class ExternalScheduleSyncJournalPersistenceSnapshot(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val records: List<ExternalScheduleSyncJournalPersistenceRecord>,
) {
    val orderedRecords: List<ExternalScheduleSyncJournalPersistenceRecord>
        get() = records.sortedWith(compareBy<ExternalScheduleSyncJournalPersistenceRecord> { it.occurredAt }.thenBy { it.journalId.value })

    val size: Int get() = records.size
}
