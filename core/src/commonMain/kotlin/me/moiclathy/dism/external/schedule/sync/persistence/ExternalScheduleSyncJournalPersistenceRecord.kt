package me.moiclathy.dism.external.schedule.sync.persistence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Sync Journal 的持久化记录。
 *
 * Journal Event 是领域对象；本类型是持久化边界上的稳定记录。
 * 不直接绑定 SQLDelight 生成代码，便于后续接入现有 DismDatabase。
 */
data class ExternalScheduleSyncJournalPersistenceRecord(
    val journalId: ExternalScheduleSyncJournalId,
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val type: ExternalScheduleSyncJournalEventType,
    val occurredAt: Instant,
    val externalKey: String? = null,
    val action: ExternalScheduleSyncAction? = null,
    val eventId: String? = null,
    val message: String? = null,
) {
    init {
        if (externalKey != null) require(externalKey.isNotBlank()) { "externalKey must not be blank" }
        if (eventId != null) require(eventId.isNotBlank()) { "eventId must not be blank" }
    }

    fun toDomain(): ExternalScheduleSyncJournalEvent =
        ExternalScheduleSyncJournalEvent(
            journalId = journalId,
            scheduleId = scheduleId,
            runId = runId,
            type = type,
            occurredAt = occurredAt,
            externalKey = externalKey,
            action = action,
            eventId = eventId,
            message = message,
        )

    companion object {
        fun fromDomain(event: ExternalScheduleSyncJournalEvent) =
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = event.journalId,
                scheduleId = event.scheduleId,
                runId = event.runId,
                type = event.type,
                occurredAt = event.occurredAt,
                externalKey = event.externalKey,
                action = event.action,
                eventId = event.eventId,
                message = event.message,
            )
    }
}
