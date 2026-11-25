package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.InMemoryExternalScheduleSyncJournalPersistenceRepository
import me.moiclathy.dism.external.schedule.sync.projection.DeterministicExternalScheduleSyncRunProjector
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunPhase
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunStatus

class ExternalScheduleSyncJournalRecoveryBootstrapTest {
    private val scheduleId = ExternalScheduleId("schedule-1")
    private val runId = ExternalScheduleSyncRunId("run-1")

    private val seedTime = Instant.parse("2026-09-18T00:59:00Z")
    private val projectedAt = Instant.parse("2026-09-18T01:03:00Z")

    /**
     * 11.27 的兼容修正版 recover 需要 seed 与 projectedAt。
     */
    private fun seed() = ExternalScheduleSyncRunRecord(
        scheduleId = scheduleId,
        runId = runId,
        status = ExternalScheduleSyncRunStatus.IN_PROGRESS,
        phase = ExternalScheduleSyncRunPhase.EXECUTING,
        startedAt = seedTime,
        updatedAt = seedTime,
        totalActions = 1,
    )

    @Test
    fun `empty journal returns empty recovery`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val service = DeterministicExternalScheduleSyncJournalRecoveryService(
            repository,
            DeterministicExternalScheduleSyncRunProjector(),
        )

        val result = service.recover(scheduleId, runId, seed(), projectedAt)

        assertEquals(ExternalScheduleSyncJournalRecoveryStatus.EMPTY, result.status)
        assertEquals(0, result.restoredEventCount)
        assertTrue(result.successful)
    }

    @Test
    fun `persisted journal is restored and projected`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val t1 = Instant.parse("2026-09-18T01:00:00Z")
        val t2 = Instant.parse("2026-09-18T01:01:00Z")
        val t3 = Instant.parse("2026-09-18T01:02:00Z")

        repository.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-1"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.ACTION_STARTED,
                occurredAt = t1,
                externalKey = "course-a",
                action = ExternalScheduleSyncAction.CREATE_EVENT,
            )
        )
        repository.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-2"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.EVENT_CREATED,
                occurredAt = t2,
                externalKey = "course-a",
                action = ExternalScheduleSyncAction.CREATE_EVENT,
                eventId = "event-a",
            )
        )
        repository.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-3"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.ACTION_COMPLETED,
                occurredAt = t3,
                externalKey = "course-a",
                action = ExternalScheduleSyncAction.CREATE_EVENT,
                eventId = "event-a",
            )
        )

        val service = DeterministicExternalScheduleSyncJournalRecoveryService(
            repository,
            DeterministicExternalScheduleSyncRunProjector(),
        )

        val result = service.recover(scheduleId, runId, seed(), projectedAt)

        assertEquals(ExternalScheduleSyncJournalRecoveryStatus.RESTORED, result.status)
        assertEquals(3, result.restoredEventCount)
        assertNotNull(result.snapshot)
        assertEquals(3, result.snapshot!!.events.size)
        assertNotNull(result.projection)
    }

    @Test
    fun `events from another schedule are ignored`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val t1 = Instant.parse("2026-09-18T01:00:00Z")

        repository.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-other"),
                scheduleId = ExternalScheduleId("other-schedule"),
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.RUN_STARTED,
                occurredAt = t1,
            )
        )

        val service = DeterministicExternalScheduleSyncJournalRecoveryService(
            repository,
            DeterministicExternalScheduleSyncRunProjector(),
        )

        val result = service.recover(scheduleId, runId, seed(), projectedAt)

        assertEquals(ExternalScheduleSyncJournalRecoveryStatus.EMPTY, result.status)
    }

    @Test
    fun `recovery ordering is deterministic`() {
        val repository = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val t = Instant.parse("2026-09-18T01:00:00Z")

        repository.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-2"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.RUN_STARTED,
                occurredAt = t,
            )
        )
        repository.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-1"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.RECOVERY_STARTED,
                occurredAt = t,
            )
        )

        val service = DeterministicExternalScheduleSyncJournalRecoveryService(
            repository,
            DeterministicExternalScheduleSyncRunProjector(),
        )

        val result = service.recover(scheduleId, runId, seed(), projectedAt)

        assertEquals(
            listOf("j-1", "j-2"),
            result.snapshot!!.events.map { it.journalId.value },
        )
    }
}
