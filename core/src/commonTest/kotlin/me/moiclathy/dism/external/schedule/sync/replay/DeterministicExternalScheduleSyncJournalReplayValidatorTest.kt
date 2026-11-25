package me.moiclathy.dism.external.schedule.sync.replay

import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.*
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

class DeterministicExternalScheduleSyncJournalReplayValidatorTest {
    private val scheduleId = ExternalScheduleId("schedule-validator")
    private val runId = ExternalScheduleSyncRunId("run-validator")
    private val t1 = Instant.parse("2026-09-20T03:00:00Z")
    private val t2 = Instant.parse("2026-09-20T03:00:01Z")
    private val t3 = Instant.parse("2026-09-20T03:00:02Z")

    private fun event(
        id: String,
        type: ExternalScheduleSyncJournalEventType,
        message: String? = null,
        time: Instant = t1,
    ) = ExternalScheduleSyncJournalEvent(
        journalId = ExternalScheduleSyncJournalId(id),
        scheduleId = scheduleId,
        runId = runId,
        type = type,
        occurredAt = time,
        externalKey = if (type.name.startsWith("ACTION")) "entry-a" else null,
        action = if (type.name.startsWith("ACTION")) ExternalScheduleSyncAction.CREATE_EVENT else null,
        eventId = null,
        message = message,
    )

    @Test
    fun `single terminal event is valid`() {
        val errors = DeterministicExternalScheduleSyncJournalReplayValidator().validate(
            scheduleId, runId,
            listOf(
                event("1", ExternalScheduleSyncJournalEventType.ACTION_STARTED),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_COMPLETED, "ok", t2),
            ),
        )
        assertFalse(errors.isNotEmpty())
    }

    @Test
    fun `failed attempt followed by successful retry is valid`() {
        val errors = DeterministicExternalScheduleSyncJournalReplayValidator().validate(
            scheduleId, runId,
            listOf(
                event("1", ExternalScheduleSyncJournalEventType.ACTION_STARTED, time = t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_FAILED, "boom", t2),
                event("3", ExternalScheduleSyncJournalEventType.ACTION_STARTED, time = t2),
                event("4", ExternalScheduleSyncJournalEventType.ACTION_COMPLETED, "ok", t3),
            ),
        )
        assertTrue(errors.isEmpty())
    }

    @Test
    fun `duplicate journal ids are rejected`() {
        val errors = DeterministicExternalScheduleSyncJournalReplayValidator().validate(
            scheduleId, runId,
            listOf(
                event("same", ExternalScheduleSyncJournalEventType.RUN_STARTED),
                event("same", ExternalScheduleSyncJournalEventType.RUN_STARTED),
            ),
        )
        assertTrue(errors.any { it.contains("Duplicate journalId") })
    }
}
