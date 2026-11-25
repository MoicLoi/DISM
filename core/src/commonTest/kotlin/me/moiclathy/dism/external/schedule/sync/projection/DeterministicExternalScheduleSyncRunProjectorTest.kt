package me.moiclathy.dism.external.schedule.sync.projection

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.*
import me.moiclathy.dism.external.schedule.sync.transaction.*

class DeterministicExternalScheduleSyncRunProjectorTest {
    private val scheduleId = ExternalScheduleId("schedule-001")
    private val runId = ExternalScheduleSyncRunId("run-001")
    private val t1 = Instant.parse("2026-09-18T01:00:00Z")
    private val t2 = Instant.parse("2026-09-18T01:01:00Z")
    private val t3 = Instant.parse("2026-09-18T01:02:00Z")
    private val t4 = Instant.parse("2026-09-18T01:03:00Z")
    private val t5 = Instant.parse("2026-09-18T01:04:00Z")

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

    private fun snapshot(vararg events: ExternalScheduleSyncJournalEvent) =
        ExternalScheduleSyncJournalSnapshot(scheduleId, runId, events.toList())

    @Test
    fun `completed journal projects to completed run`() {
        val key = "entry-a"
        val action = ExternalScheduleSyncAction.CREATE_EVENT
        val result = DeterministicExternalScheduleSyncRunProjector().project(
            seed(),
            snapshot(
                event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t2, key, action),
                event("3", ExternalScheduleSyncJournalEventType.EVENT_CREATED, t3, key, action, "event-a"),
                event("4", ExternalScheduleSyncJournalEventType.MAPPING_COMMITTED, t4, key, action, "event-a"),
                event("5", ExternalScheduleSyncJournalEventType.ACTION_COMPLETED, t5, key, action, "event-a"),
                event("6", ExternalScheduleSyncJournalEventType.RUN_COMPLETED, t5),
            ),
            t5,
        )
        assertTrue(result.successful)
        val record = result.projection!!.record
        assertEquals(ExternalScheduleSyncRunStatus.COMPLETED, record.status)
        assertEquals(ExternalScheduleSyncRunPhase.COMPLETED, record.phase)
        assertEquals(1, record.totalActions)
        assertEquals(1, record.completedActions)
        assertEquals(0, record.failedActions)
        assertEquals(t5, record.completedAt)
    }

    @Test
    fun `started action projects to in progress`() {
        val action = ExternalScheduleSyncAction.CREATE_EVENT
        val result = DeterministicExternalScheduleSyncRunProjector().project(
            seed(),
            snapshot(
                event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t2, "entry-a", action),
            ),
            t2,
        )
        assertTrue(result.successful)
        assertEquals(ExternalScheduleSyncRunStatus.IN_PROGRESS, result.projection!!.record.status)
        assertEquals(ExternalScheduleSyncRunPhase.EXECUTING, result.projection.record.phase)
        assertEquals(1, result.projection.pendingActionCount)
    }

    @Test
    fun `failed action with no pending actions projects to failed`() {
        val action = ExternalScheduleSyncAction.CREATE_EVENT
        val result = DeterministicExternalScheduleSyncRunProjector().project(
            seed(),
            snapshot(
                event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t2, "entry-a", action),
                event("3", ExternalScheduleSyncJournalEventType.ACTION_FAILED, t3, "entry-a", action, message = "create failed"),
            ),
            t3,
        )
        assertTrue(result.successful)
        val record = result.projection!!.record
        assertEquals(ExternalScheduleSyncRunStatus.FAILED, record.status)
        assertEquals(1, record.failedActions)
        assertEquals("create failed", record.message)
    }

    @Test
    fun `failed action with another pending action projects to partial`() {
        val action = ExternalScheduleSyncAction.CREATE_EVENT
        val result = DeterministicExternalScheduleSyncRunProjector().project(
            seed(2),
            snapshot(
                event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t2, "entry-a", action),
                event("3", ExternalScheduleSyncJournalEventType.ACTION_FAILED, t3, "entry-a", action, message = "failed"),
                event("4", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t4, "entry-b", action),
            ),
            t4,
        )
        assertTrue(result.successful)
        assertEquals(ExternalScheduleSyncRunStatus.PARTIAL, result.projection!!.record.status)
        assertEquals(1, result.projection.pendingActionCount)
    }

    @Test
    fun `recovery completion projects to recovered`() {
        val action = ExternalScheduleSyncAction.UPDATE_EVENT
        val result = DeterministicExternalScheduleSyncRunProjector().project(
            seed(),
            snapshot(
                event("1", ExternalScheduleSyncJournalEventType.RECOVERY_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_STARTED, t2, "entry-a", action),
                event("3", ExternalScheduleSyncJournalEventType.ACTION_COMPLETED, t3, "entry-a", action, "event-a"),
                event("4", ExternalScheduleSyncJournalEventType.RECOVERY_COMPLETED, t4),
            ),
            t4,
        )
        assertTrue(result.successful)
        assertEquals(ExternalScheduleSyncRunStatus.RECOVERED, result.projection!!.record.status)
        assertEquals(ExternalScheduleSyncRunPhase.COMPLETED, result.projection.record.phase)
        assertEquals(t4, result.projection.record.completedAt)
    }

    @Test
    fun `zero action run projects to no changes`() {
        val result = DeterministicExternalScheduleSyncRunProjector().project(
            seed(0),
            snapshot(
                event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.RUN_COMPLETED, t2),
            ),
            t2,
        )
        assertTrue(result.successful)
        assertEquals(ExternalScheduleSyncRunStatus.NO_CHANGES, result.projection!!.record.status)
        assertEquals(ExternalScheduleSyncRunPhase.COMPLETED, result.projection.record.phase)
        assertEquals(t2, result.projection.record.completedAt)
    }

    @Test
    fun `skipped action is terminal but not counted as completed action`() {
        val result = DeterministicExternalScheduleSyncRunProjector().project(
            seed(),
            snapshot(
                event("1", ExternalScheduleSyncJournalEventType.RUN_STARTED, t1),
                event("2", ExternalScheduleSyncJournalEventType.ACTION_SKIPPED, t2, "entry-a", ExternalScheduleSyncAction.NO_OP),
                event("3", ExternalScheduleSyncJournalEventType.RUN_COMPLETED, t3),
            ),
            t3,
        )
        assertTrue(result.successful)
        assertEquals(ExternalScheduleSyncRunStatus.COMPLETED, result.projection!!.record.status)
        assertEquals(0, result.projection.record.completedActions)
        assertEquals(1, result.projection.skippedActionCount)
        assertEquals(0, result.projection.pendingActionCount)
    }

    @Test
    fun `mismatched seed is rejected`() {
        val otherRun = ExternalScheduleSyncRunId("run-002")
        val otherSeed = ExternalScheduleSyncRunRecord(
            scheduleId = scheduleId,
            runId = otherRun,
            status = ExternalScheduleSyncRunStatus.CREATED,
            phase = ExternalScheduleSyncRunPhase.CREATED,
            startedAt = t1,
            updatedAt = t1,
            totalActions = 1,
        )
        val result = DeterministicExternalScheduleSyncRunProjector().project(otherSeed, snapshot(), t1)
        assertTrue(!result.successful)
    }
}
