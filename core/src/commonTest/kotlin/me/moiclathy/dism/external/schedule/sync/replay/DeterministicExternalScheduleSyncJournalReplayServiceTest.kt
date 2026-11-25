package me.moiclathy.dism.external.schedule.sync.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.*
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceService
import me.moiclathy.dism.external.schedule.sync.persistence.InMemoryExternalScheduleSyncJournalPersistenceRepository
import me.moiclathy.dism.external.schedule.sync.transaction.*

class DeterministicExternalScheduleSyncJournalReplayServiceTest {
    private val scheduleId = ExternalScheduleId("schedule-1133")
    private val runId = ExternalScheduleSyncRunId("run-1133")
    private val t1 = Instant.parse("2026-09-20T02:00:00Z")
    private val t2 = Instant.parse("2026-09-20T02:01:00Z")
    private val t3 = Instant.parse("2026-09-20T02:02:00Z")
    private val t4 = Instant.parse("2026-09-20T02:03:00Z")

    private fun seed(total: Int = 1) = ExternalScheduleSyncRunRecord(
        scheduleId = scheduleId,
        runId = runId,
        status = ExternalScheduleSyncRunStatus.CREATED,
        phase = ExternalScheduleSyncRunPhase.CREATED,
        startedAt = t1,
        updatedAt = t1,
        totalActions = total,
    )

    private fun event(
        id: String,
        type: ExternalScheduleSyncJournalEventType,
        at: Instant,
        key: String? = null,
        action: ExternalScheduleSyncAction? = null,
        eventId: String? = null,
        message: String? = null,
    ) = ExternalScheduleSyncJournalEvent(
        journalId = ExternalScheduleSyncJournalId(id),
        scheduleId = scheduleId,
        runId = runId,
        type = type,
        occurredAt = at,
        externalKey = key,
        action = action,
        eventId = eventId,
        message = message,
    )

    private fun service(
        repository: InMemoryExternalScheduleSyncJournalPersistenceRepository,
    ) = DeterministicExternalScheduleSyncJournalReplayService(
        journalPersistence = ExternalScheduleSyncJournalPersistenceService(repository),
    )

    @Test
    fun `full replay rehydrates projection`() {
        val repo = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val action = ExternalScheduleSyncAction.CREATE_EVENT
        ExternalScheduleSyncJournalPersistenceService(repo).appendAll(
            listOf(
                event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t2, "entry-a", action),
                event("3", ExternalScheduleSyncJournalEventType.EVENT_CREATED, t3, "entry-a", action, "event-a"),
                event("4", ExternalScheduleSyncJournalEventType.ACTION_COMPLETED, t4, "entry-a", action, "event-a"),
                event("5", ExternalScheduleSyncJournalEventType.RUN_COMPLETED, t4),
            ),
        )

        val result = service(repo).replay(
            ExternalScheduleSyncJournalReplayRequest(
                scheduleId, runId, seed(), t4,
            ),
        )

        assertEquals(ExternalScheduleSyncJournalReplayStatus.REPLAYED, result.status)
        assertEquals(5, result.replayedEventCount)
        assertEquals(1, result.checkpoints.size)
        assertEquals(ExternalScheduleSyncCheckpointStatus.COMPLETED, result.checkpoints.single().status)
        assertEquals(ExternalScheduleSyncRunStatus.COMPLETED, result.projection?.record?.status)
    }

    @Test
    fun `checkpoint rehydration does not project run`() {
        val repo = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val action = ExternalScheduleSyncAction.UPDATE_EVENT
        ExternalScheduleSyncJournalPersistenceService(repo).appendAll(
            listOf(
                event("1", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t1, "entry-a", action),
            ),
        )

        val result = service(repo).replay(
            ExternalScheduleSyncJournalReplayRequest(
                scheduleId, runId, seed(), t2,
                ExternalScheduleSyncJournalReplayMode.CHECKPOINT_REHYDRATION,
            ),
        )

        assertEquals(ExternalScheduleSyncJournalReplayStatus.PARTIAL, result.status)
        assertEquals(1, result.checkpoints.size)
        assertTrue(result.projection == null)
    }

    @Test
    fun `validate only does not project`() {
        val repo = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        ExternalScheduleSyncJournalPersistenceService(repo).append(
            event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
        )

        val result = service(repo).replay(
            ExternalScheduleSyncJournalReplayRequest(
                scheduleId, runId, seed(0), t2,
                ExternalScheduleSyncJournalReplayMode.VALIDATE_ONLY,
            ),
        )

        assertEquals(ExternalScheduleSyncJournalReplayStatus.REPLAYED, result.status)
        assertTrue(result.projection == null)
    }

    @Test
    fun `empty journal is reported without inventing state`() {
        val result = service(InMemoryExternalScheduleSyncJournalPersistenceRepository()).replay(
            ExternalScheduleSyncJournalReplayRequest(scheduleId, runId, seed(), t1),
        )

        assertEquals(ExternalScheduleSyncJournalReplayStatus.EMPTY, result.status)
        assertTrue(result.projection == null)
        assertEquals(0, result.replayedEventCount)
    }
}
