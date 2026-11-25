package me.moiclathy.dism.external.schedule.sync.persistence.sqldelight

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRecord
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

class ExternalScheduleSyncJournalSqlDelightCodecTest {

    @Test
    fun `domain record binds to stable sql values`() {
        val record = ExternalScheduleSyncJournalPersistenceRecord(
            journalId = ExternalScheduleSyncJournalId("journal-1"),
            scheduleId = ExternalScheduleId("schedule-1"),
            runId = ExternalScheduleSyncRunId("run-1"),
            type = ExternalScheduleSyncJournalEventType.EVENT_CREATED,
            occurredAt = Instant.parse("2026-09-18T01:02:03Z"),
            externalKey = "course-a",
            action = ExternalScheduleSyncAction.CREATE_EVENT,
            eventId = "event-1",
            message = "created",
        )

        val values = ExternalScheduleSyncJournalSqlDelightCodec.bind(record)

        assertEquals("journal-1", values.journalId)
        assertEquals("schedule-1", values.scheduleId)
        assertEquals("run-1", values.runId)
        assertEquals("EVENT_CREATED", values.eventType)
        assertEquals(record.occurredAt.toEpochMilliseconds(), values.occurredAtEpochMs)
        assertEquals("course-a", values.externalKey)
        assertEquals("CREATE_EVENT", values.action)
        assertEquals("event-1", values.eventId)
        assertEquals("created", values.message)
    }

    @Test
    fun `domain event can become persistence record before sql binding`() {
        val event = ExternalScheduleSyncJournalEvent(
            journalId = ExternalScheduleSyncJournalId("journal-2"),
            scheduleId = ExternalScheduleId("schedule-2"),
            runId = ExternalScheduleSyncRunId("run-2"),
            type = ExternalScheduleSyncJournalEventType.ACTION_COMPLETED,
            occurredAt = Instant.parse("2026-09-18T02:00:00Z"),
            externalKey = "course-b",
            action = ExternalScheduleSyncAction.UPDATE_EVENT,
            eventId = "event-2",
        )

        val record = ExternalScheduleSyncJournalPersistenceRecord.fromDomain(event)
        val values = ExternalScheduleSyncJournalSqlDelightCodec.bind(record)

        assertEquals(event.journalId.value, values.journalId)
        assertEquals(event.scheduleId.value, values.scheduleId)
        assertEquals(event.runId.value, values.runId)
        assertEquals(event.type.name, values.eventType)
        assertEquals(event.action?.name, values.action)
        assertEquals(event.eventId, values.eventId)
    }
}
