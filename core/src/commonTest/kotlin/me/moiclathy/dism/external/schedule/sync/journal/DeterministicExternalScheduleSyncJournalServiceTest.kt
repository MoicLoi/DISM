package me.moiclathy.dism.external.schedule.sync.journal

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeterministicExternalScheduleSyncJournalServiceTest {

    private val scheduleId =
        ExternalScheduleId(
            "schedule-001"
        )

    private val runId =
        ExternalScheduleSyncRunId(
            "run-001"
        )

    private val t1 =
        Instant.parse(
            "2026-09-10T01:00:00Z"
        )

    private val t2 =
        Instant.parse(
            "2026-09-10T01:01:00Z"
        )

    private val t3 =
        Instant.parse(
            "2026-09-10T01:02:00Z"
        )

    private val t4 =
        Instant.parse(
            "2026-09-10T01:03:00Z"
        )

    private val t5 =
        Instant.parse(
            "2026-09-10T01:04:00Z"
        )

    @Test
    fun `run journal events are persisted`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordRunStarted(
            scheduleId,
            runId,
            t1,
        )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t2,
        )

        val events =
            repository.findByRun(
                runId
            )

        assertEquals(
            2,
            events.size
        )

        assertEquals(
            ExternalScheduleSyncJournalEventType
                .RUN_STARTED,

            events[0].type
        )

        assertEquals(
            ExternalScheduleSyncJournalEventType
                .ACTION_STARTED,

            events[1].type
        )
    }

    @Test
    fun `create event checkpoint is projected`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t1,
        )

        service.recordEventCreated(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t2,
        )

        val snapshot =
            service.snapshot(
                scheduleId,
                runId,
            )

        val checkpoint =
            snapshot.checkpoints.single()

        assertEquals(
            ExternalScheduleSyncCheckpointStatus
                .EVENT_CREATED,

            checkpoint.status
        )

        assertEquals(
            "event-a",
            checkpoint.eventId
        )

        assertTrue(
            !checkpoint.terminal
        )
    }

    @Test
    fun `mapping committed checkpoint is projected`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t1,
        )

        service.recordEventCreated(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t2,
        )

        service.recordMappingCommitted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t3,
        )

        val checkpoint =
            service
                .snapshot(
                    scheduleId,
                    runId,
                )
                .checkpoints
                .single()

        assertEquals(
            ExternalScheduleSyncCheckpointStatus
                .MAPPING_COMMITTED,

            checkpoint.status
        )

        assertEquals(
            "event-a",
            checkpoint.eventId
        )
    }

    @Test
    fun `completed checkpoint is terminal`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t1,
        )

        service.recordEventCreated(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t2,
        )

        service.recordMappingCommitted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t3,
        )

        service.recordActionCompleted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t4,
        )

        val checkpoint =
            service
                .snapshot(
                    scheduleId,
                    runId,
                )
                .checkpoints
                .single()

        assertEquals(
            ExternalScheduleSyncCheckpointStatus
                .COMPLETED,

            checkpoint.status
        )

        assertTrue(
            checkpoint.terminal
        )
    }

    @Test
    fun `failed checkpoint contains failure message`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t1,
        )

        service.recordActionFailed(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "creation failed",
            t2,
        )

        val checkpoint =
            service
                .snapshot(
                    scheduleId,
                    runId,
                )
                .checkpoints
                .single()

        assertEquals(
            ExternalScheduleSyncCheckpointStatus
                .FAILED,

            checkpoint.status
        )

        assertEquals(
            "creation failed",
            checkpoint.message
        )

        assertTrue(
            checkpoint.terminal
        )
    }

    @Test
    fun `journal projection is deterministic`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-b",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t2,
        )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t1,
        )

        val snapshot =
            service.snapshot(
                scheduleId,
                runId,
            )

        assertEquals(
            listOf(
                "entry-a",
                "entry-b",
            ),

            snapshot.checkpoints.map {
                it.externalKey
            }
        )
    }

    @Test
    fun `pending count identifies interrupted action`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t1,
        )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-b",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t2,
        )

        service.recordActionCompleted(
            scheduleId,
            runId,
            "entry-b",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-b",
            t3,
        )

        val snapshot =
            service.snapshot(
                scheduleId,
                runId,
            )

        assertEquals(
            1,
            snapshot.pendingCount
        )

        assertEquals(
            1,
            snapshot.completedCount
        )
    }

    @Test
    fun `skipped action becomes terminal`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionSkipped(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.NO_OP,
            t1,
        )

        val checkpoint =
            service
                .snapshot(
                    scheduleId,
                    runId,
                )
                .checkpoints
                .single()

        assertEquals(
            ExternalScheduleSyncCheckpointStatus
                .SKIPPED,

            checkpoint.status
        )

        assertTrue(
            checkpoint.terminal
        )
    }

    @Test
    fun `event updated checkpoint is represented`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.UPDATE_EVENT,
            t1,
        )

        service.recordEventUpdated(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.UPDATE_EVENT,
            "event-a",
            t2,
        )

        val checkpoint =
            service
                .snapshot(
                    scheduleId,
                    runId,
                )
                .checkpoints
                .single()

        assertEquals(
            ExternalScheduleSyncCheckpointStatus
                .EVENT_UPDATED,

            checkpoint.status
        )

        assertEquals(
            "event-a",
            checkpoint.eventId
        )
    }

    @Test
    fun `snapshot contains original journal events`() {

        val repository =
            InMemoryExternalScheduleSyncJournalRepository()

        val service =
            DeterministicExternalScheduleSyncJournalService(
                repository
            )

        service.recordRunStarted(
            scheduleId,
            runId,
            t1,
        )

        service.recordActionStarted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            t2,
        )

        service.recordEventCreated(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t3,
        )

        service.recordMappingCommitted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t4,
        )

        service.recordActionCompleted(
            scheduleId,
            runId,
            "entry-a",
            ExternalScheduleSyncAction.CREATE_EVENT,
            "event-a",
            t5,
        )

        val snapshot =
            service.snapshot(
                scheduleId,
                runId,
            )

        assertEquals(
            5,
            snapshot.events.size
        )

        assertNotNull(
            snapshot.checkpoints.single()
        )
    }
}
