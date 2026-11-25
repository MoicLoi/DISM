package me.moiclathy.dism.external.schedule.sync.reconciliation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.persistence.InMemoryExternalSchedulePersistenceRepository
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncExecutionStatus
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord
import me.moiclathy.dism.external.schedule.sync.persistence.InMemoryExternalScheduleSyncPersistenceRepository
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryCoordinator
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryResult
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryStatus
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunPhase
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunStatus

class ExternalScheduleSyncRecoveryReconciliationCoordinatorTest {

    private val scheduleId = ExternalScheduleId("schedule-11-28")
    private val runId = ExternalScheduleSyncRunId("run-11-28")
    private val now = Instant.parse("2026-09-19T02:00:00Z")

    private val seed = ExternalScheduleSyncRunRecord(
        scheduleId = scheduleId,
        runId = runId,
        status = ExternalScheduleSyncRunStatus.IN_PROGRESS,
        phase = ExternalScheduleSyncRunPhase.EXECUTING,
        startedAt = now,
        updatedAt = now,
        totalActions = 1,
    )

    @Test
    fun emptyJournalStillReconcilesDurableState() {
        val recovery = object : ExternalScheduleSyncRunRecoveryCoordinator {
            override fun recover(
                scheduleId: ExternalScheduleId,
                runId: ExternalScheduleSyncRunId,
                seed: ExternalScheduleSyncRunRecord,
                projectedAt: Instant,
            ) = ExternalScheduleSyncRunRecoveryResult(
                scheduleId = scheduleId,
                runId = runId,
                status = ExternalScheduleSyncRunRecoveryStatus.EMPTY,
                persistedRun = seed,
            )
        }

        val currentRepository = InMemoryExternalSchedulePersistenceRepository()
        currentRepository.upsert(
            ExternalSchedulePersistenceRecord(
                externalScheduleId = scheduleId,
                externalKey = "course-a",
                eventId = "event-a",
                title = "Course A",
                startAt = now,
                endAt = now + 1.hours,
                lastSeenAt = now,
            ),
        )

        val syncRepository = InMemoryExternalScheduleSyncPersistenceRepository()
        syncRepository.save(
            ExternalScheduleSyncPersistenceRecord(
                scheduleId = scheduleId,
                externalKey = "course-a",
                action = ExternalScheduleSyncAction.CREATE_EVENT,
                status = ExternalScheduleSyncExecutionStatus.CREATED,
                eventId = "event-a",
                executedAt = now,
                syncRunId = runId.value,
            ),
        )

        val reconciliation = DeterministicExternalScheduleSyncReconciliationService(
            ExternalScheduleSyncEventLookup { eventId -> eventId == "event-a" },
        )

        val coordinator = DeterministicExternalScheduleSyncRecoveryReconciliationCoordinator(
            recoveryCoordinator = recovery,
            externalScheduleRepository = currentRepository,
            syncPersistenceRepository = syncRepository,
            reconciliationService = reconciliation,
        )

        val result = coordinator.reconcile(
            scheduleId = scheduleId,
            runId = runId,
            seed = seed,
            projectedAt = now,
        )

        assertEquals(
            ExternalScheduleSyncRecoveryReconciliationStatus.RECOVERY_EMPTY,
            result.status,
        )
        assertTrue(result.successful)
        assertEquals(0, result.actionRequiredCount)
        assertEquals(1, result.consistentCount)
    }

    @Test
    fun seedMismatchIsRejectedBeforeRecovery() {
        var called = false

        val recovery = object : ExternalScheduleSyncRunRecoveryCoordinator {
            override fun recover(
                scheduleId: ExternalScheduleId,
                runId: ExternalScheduleSyncRunId,
                seed: ExternalScheduleSyncRunRecord,
                projectedAt: Instant,
            ): ExternalScheduleSyncRunRecoveryResult {
                called = true
                error("recovery must not be called")
            }
        }

        val coordinator = DeterministicExternalScheduleSyncRecoveryReconciliationCoordinator(
            recoveryCoordinator = recovery,
            externalScheduleRepository = InMemoryExternalSchedulePersistenceRepository(),
            syncPersistenceRepository = InMemoryExternalScheduleSyncPersistenceRepository(),
            reconciliationService = DeterministicExternalScheduleSyncReconciliationService(
                ExternalScheduleSyncEventLookup { true },
            ),
        )

        val result = coordinator.reconcile(
            scheduleId = scheduleId,
            runId = runId,
            seed = seed.copy(runId = ExternalScheduleSyncRunId("other-run")),
            projectedAt = now,
        )

        assertEquals(ExternalScheduleSyncRecoveryReconciliationStatus.FAILED, result.status)
        assertTrue(result.errors.any { it.contains("runId") })
        assertTrue(!called)
    }
}
