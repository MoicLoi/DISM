package me.moiclathy.dism.external.schedule.sync.persistence.sqldelight

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRepository
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceSnapshot
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.persistence.db.ExternalScheduleSyncJournalQueries

/**
 * 11.24 Journal Persistence Boundary 的 SQLDelight 实现。
 *
 * 注意：这里依赖的是 generated Queries，而不是 DismDatabase 本身。
 * 这样可以保持 infrastructure adapter 与具体 database composition 解耦。
 */
class SqlDelightExternalScheduleSyncJournalPersistenceRepository(
    private val queries: ExternalScheduleSyncJournalQueries,
) : ExternalScheduleSyncJournalPersistenceRepository {

    override fun append(
        record: ExternalScheduleSyncJournalPersistenceRecord,
    ): ExternalScheduleSyncJournalPersistenceRecord {
        val values = ExternalScheduleSyncJournalSqlDelightCodec.bind(record)
        queries.insertOrIgnore(
            journal_id = values.journalId,
            schedule_id = values.scheduleId,
            run_id = values.runId,
            event_type = values.eventType,
            occurred_at_epoch_ms = values.occurredAtEpochMs,
            external_key = values.externalKey,
            action = values.action,
            event_id = values.eventId,
            message = values.message,
        )
        return find(record.journalId) ?: record
    }

    override fun appendAll(
        records: List<ExternalScheduleSyncJournalPersistenceRecord>,
    ): List<ExternalScheduleSyncJournalPersistenceRecord> =
        records.map(::append)

    override fun find(
        journalId: ExternalScheduleSyncJournalId,
    ): ExternalScheduleSyncJournalPersistenceRecord? =
        queries.selectByJournalId(journalId.value)
            .executeAsOneOrNull()
            ?.let(ExternalScheduleSyncJournalSqlDelightCodec::toRecord)

    override fun findByRun(
        runId: ExternalScheduleSyncRunId,
    ): List<ExternalScheduleSyncJournalPersistenceRecord> =
        queries.selectByRunId(runId.value)
            .executeAsList()
            .map(ExternalScheduleSyncJournalSqlDelightCodec::toRecord)

    override fun findBySchedule(
        scheduleId: ExternalScheduleId,
    ): List<ExternalScheduleSyncJournalPersistenceRecord> =
        queries.selectByScheduleId(scheduleId.value)
            .executeAsList()
            .map(ExternalScheduleSyncJournalSqlDelightCodec::toRecord)

    override fun snapshot(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
    ): ExternalScheduleSyncJournalPersistenceSnapshot =
        ExternalScheduleSyncJournalPersistenceSnapshot(
            scheduleId = scheduleId,
            runId = runId,
            records = queries.selectByScheduleAndRun(
                schedule_id = scheduleId.value,
                run_id = runId.value,
            )
                .executeAsList()
                .map(ExternalScheduleSyncJournalSqlDelightCodec::toRecord),
        )

    override fun deleteRun(
        runId: ExternalScheduleSyncRunId,
    ): Int {
        val before = queries.selectByRunId(runId.value).executeAsList().size
        queries.deleteByRunId(runId.value)
        return before
    }
}
