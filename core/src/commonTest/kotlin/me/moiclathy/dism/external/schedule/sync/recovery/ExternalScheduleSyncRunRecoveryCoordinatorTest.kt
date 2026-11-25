package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
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
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRepository
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunStatus

class ExternalScheduleSyncRunRecoveryCoordinatorTest {
    private val scheduleId = ExternalScheduleId("schedule-1")
    private val runId = ExternalScheduleSyncRunId("run-1")
    private val t1 = Instant.parse("2026-09-18T01:00:00Z")
    private val t2 = Instant.parse("2026-09-18T01:01:00Z")
    private val t3 = Instant.parse("2026-09-18T01:02:00Z")

    private fun seed() = ExternalScheduleSyncRunRecord(
        scheduleId = scheduleId,
        runId = runId,
        status = ExternalScheduleSyncRunStatus.IN_PROGRESS,
        phase = ExternalScheduleSyncRunPhase.EXECUTING,
        startedAt = t1,
        updatedAt = t1,
        totalActions = 1,
    )

    @Test
    fun `empty journal retains seed without writing fake projection`() {
        val journal = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val runs = RecordingRunRepository()
        val recovery = DeterministicExternalScheduleSyncJournalRecoveryService(
            journal,
            DeterministicExternalScheduleSyncRunProjector(),
        )
        val coordinator = DeterministicExternalScheduleSyncRunRecoveryCoordinator(recovery, runs)

        val result = coordinator.recover(scheduleId, runId, seed(), t2)

        assertEquals(ExternalScheduleSyncRunRecoveryStatus.EMPTY, result.status)
        assertEquals(seed(), result.persistedRun)
        assertTrue(runs.saved.isEmpty())
    }

    @Test
    fun `completed journal is projected and persisted`() {
        val journal = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        journal.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-1"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.RUN_STARTED,
                occurredAt = t1,
            )
        )
        journal.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-2"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.ACTION_STARTED,
                occurredAt = t2,
                externalKey = "course-a",
                action = ExternalScheduleSyncAction.CREATE_EVENT,
            )
        )
        journal.append(
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
        journal.append(
            ExternalScheduleSyncJournalPersistenceRecord(
                journalId = ExternalScheduleSyncJournalId("j-4"),
                scheduleId = scheduleId,
                runId = runId,
                type = ExternalScheduleSyncJournalEventType.RUN_COMPLETED,
                occurredAt = t3,
            )
        )

        val runs = RecordingRunRepository()
        val recovery = DeterministicExternalScheduleSyncJournalRecoveryService(
            journal,
            DeterministicExternalScheduleSyncRunProjector(),
        )
        val coordinator = DeterministicExternalScheduleSyncRunRecoveryCoordinator(recovery, runs)

        val result = coordinator.recover(scheduleId, runId, seed(), t3)

        assertEquals(ExternalScheduleSyncRunRecoveryStatus.PERSISTED, result.status)
        assertEquals(4, result.restoredEventCount)
        assertEquals(ExternalScheduleSyncRunStatus.COMPLETED, result.persistedRun!!.status)
        assertEquals(1, runs.saved.size)
    }

    @Test
    fun `seed mismatch is rejected`() {
        val journal = InMemoryExternalScheduleSyncJournalPersistenceRepository()
        val runs = RecordingRunRepository()
        val recovery = DeterministicExternalScheduleSyncJournalRecoveryService(
            journal,
            DeterministicExternalScheduleSyncRunProjector(),
        )
        val coordinator = DeterministicExternalScheduleSyncRunRecoveryCoordinator(recovery, runs)
        val otherSeed = seed().copy(runId = ExternalScheduleSyncRunId("other"))

        val result = coordinator.recover(scheduleId, runId, otherSeed, t2)

        assertEquals(ExternalScheduleSyncRunRecoveryStatus.FAILED, result.status)
        assertTrue(runs.saved.isEmpty())
    }

    private class RecordingRunRepository : ExternalScheduleSyncRunRepository {
        val saved = mutableListOf<ExternalScheduleSyncRunRecord>()

        override fun find(runId: ExternalScheduleSyncRunId): ExternalScheduleSyncRunRecord? =
            saved.lastOrNull { it.runId == runId }

        override fun findLatest(scheduleId: ExternalScheduleId): ExternalScheduleSyncRunRecord? =
            saved.lastOrNull { it.scheduleId == scheduleId }

        override fun save(record: ExternalScheduleSyncRunRecord): ExternalScheduleSyncRunRecord {
            saved += record
            return record
        }

        override fun delete(runId: ExternalScheduleSyncRunId) {
            saved.removeAll { it.runId == runId }
        }
    }
}
