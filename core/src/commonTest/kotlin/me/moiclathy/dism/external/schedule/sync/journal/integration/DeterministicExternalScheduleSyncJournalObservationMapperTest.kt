package me.moiclathy.dism.external.schedule.sync.journal.integration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.e2e.ExternalScheduleSyncE2EActionTrace
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionStatus
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

class DeterministicExternalScheduleSyncJournalObservationMapperTest {
    private val scheduleId = ExternalScheduleId("schedule-1")
    private val runId = ExternalScheduleSyncRunId("run-1")
    private val now = Instant.fromEpochMilliseconds(1000L)
    private val mapper = DeterministicExternalScheduleSyncJournalObservationMapper()

    @Test
    fun `create execution produces started created and completed facts`() {
        val trace = ExternalScheduleSyncE2EActionTrace(
            externalKey = "course-a",
            reconciliationAction = ExternalScheduleSyncReconciliationAction.RETRY_CREATE,
            syncAction = ExternalScheduleSyncAction.CREATE_EVENT,
            executionStatus = ExternalScheduleSyncActionExecutionStatus.EXECUTED,
            eventId = "event-a",
        )

        val events = mapper.mapAction(scheduleId, runId, trace, now, 2)

        assertEquals(
            listOf(
                ExternalScheduleSyncJournalEventType.ACTION_STARTED,
                ExternalScheduleSyncJournalEventType.EVENT_CREATED,
                ExternalScheduleSyncJournalEventType.ACTION_COMPLETED,
            ),
            events.map { it.type },
        )
        assertEquals(listOf(2, 3, 4), events.map { it.journalId.value.substringAfter(":").split(":")[1].toInt() })
    }

    @Test
    fun `no op is observed without entering action journal`() {
        val trace = ExternalScheduleSyncE2EActionTrace(
            externalKey = "course-a",
            reconciliationAction = ExternalScheduleSyncReconciliationAction.NO_OP,
        )

        val events = mapper.mapAction(scheduleId, runId, trace, now, 5)

        assertEquals(1, events.size)
        assertEquals(ExternalScheduleSyncJournalEventType.RECONCILIATION_OBSERVED, events.single().type)
        assertEquals(null, events.single().action)
    }

    @Test
    fun `failed action emits action failed with a message`() {
        val trace = ExternalScheduleSyncE2EActionTrace(
            externalKey = "course-a",
            reconciliationAction = ExternalScheduleSyncReconciliationAction.RETRY_UPDATE,
            syncAction = ExternalScheduleSyncAction.UPDATE_EVENT,
            executionStatus = ExternalScheduleSyncActionExecutionStatus.FAILED,
            errors = listOf("update failed"),
        )

        val events = mapper.mapAction(scheduleId, runId, trace, now, 0)

        assertTrue(events.last().type == ExternalScheduleSyncJournalEventType.ACTION_FAILED)
        assertEquals("update failed", events.last().message)
    }
}
