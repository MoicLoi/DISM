package me.moiclathy.dism.external.schedule.sync.persistence.sqldelight

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRecord
import me.moiclathy.dism.persistence.db.External_schedule_sync_journal

/**
 * SQLDelight row 与 DISM Journal persistence record 之间的确定性转换。
 *
 * 数据库只保存稳定 primitive：String / Long / nullable String。
 * Domain enum/value objects 不进入 SQLDelight schema。
 */
object ExternalScheduleSyncJournalSqlDelightCodec {

    fun toRecord(row: External_schedule_sync_journal): ExternalScheduleSyncJournalPersistenceRecord =
        ExternalScheduleSyncJournalPersistenceRecord(
            journalId = ExternalScheduleSyncJournalId(row.journal_id),
            scheduleId = ExternalScheduleId(row.schedule_id),
            runId = ExternalScheduleSyncRunId(row.run_id),
            type = parseEventType(row.event_type),
            occurredAt = Instant.fromEpochMilliseconds(row.occurred_at_epoch_ms),
            externalKey = row.external_key,
            action = row.action?.let(::parseAction),
            eventId = row.event_id,
            message = row.message,
        )

    fun bind(record: ExternalScheduleSyncJournalPersistenceRecord): SqlDelightJournalValues =
        SqlDelightJournalValues(
            journalId = record.journalId.value,
            scheduleId = record.scheduleId.value,
            runId = record.runId.value,
            eventType = record.type.name,
            occurredAtEpochMs = record.occurredAt.toEpochMilliseconds(),
            externalKey = record.externalKey,
            action = record.action?.name,
            eventId = record.eventId,
            message = record.message,
        )

    private fun parseEventType(value: String): ExternalScheduleSyncJournalEventType =
        runCatching { ExternalScheduleSyncJournalEventType.valueOf(value) }
            .getOrElse {
                throw IllegalStateException("Unknown journal event type: $value", it)
            }

    private fun parseAction(value: String): ExternalScheduleSyncAction =
        runCatching { ExternalScheduleSyncAction.valueOf(value) }
            .getOrElse {
                throw IllegalStateException("Unknown sync action: $value", it)
            }
}

data class SqlDelightJournalValues(
    val journalId: String,
    val scheduleId: String,
    val runId: String,
    val eventType: String,
    val occurredAtEpochMs: Long,
    val externalKey: String?,
    val action: String?,
    val eventId: String?,
    val message: String?,
)
