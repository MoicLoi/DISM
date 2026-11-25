package me.moiclathy.dism.external.schedule.sync.reconciliation

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRepository
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRepository
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryCoordinator
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryResult
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryStatus
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

/**
 * 11.28:
 *
 * 11.27 Run Recovery
 *        ↓
 * current external schedule state + sync history
 *        ↓
 * 11.20 Reconciliation
 *
 * This class is deliberately a decision boundary. It never mutates Event state.
 */
class DeterministicExternalScheduleSyncRecoveryReconciliationCoordinator(
    private val recoveryCoordinator: ExternalScheduleSyncRunRecoveryCoordinator,
    private val externalScheduleRepository: ExternalSchedulePersistenceRepository,
    private val syncPersistenceRepository: ExternalScheduleSyncPersistenceRepository,
    private val reconciliationService: ExternalScheduleSyncReconciliationService,
) : ExternalScheduleSyncRecoveryReconciliationCoordinator {

    override fun reconcile(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncRecoveryReconciliationResult {
        if (seed.scheduleId != scheduleId) {
            return failure(
                scheduleId,
                runId,
                seed,
                "seed scheduleId does not match requested scheduleId",
            )
        }

        if (seed.runId != runId) {
            return failure(
                scheduleId,
                runId,
                seed,
                "seed runId does not match requested runId",
            )
        }

        val recovery = recoveryCoordinator.recover(
            scheduleId = scheduleId,
            runId = runId,
            seed = seed,
            projectedAt = projectedAt,
        )

        if (recovery.status == ExternalScheduleSyncRunRecoveryStatus.FAILED) {
            return ExternalScheduleSyncRecoveryReconciliationResult(
                status = ExternalScheduleSyncRecoveryReconciliationStatus.FAILED,
                recovery = recovery,
                projection = recovery.projection,
                warnings = recovery.warnings,
                errors = recovery.errors,
            )
        }

        val currentRecords = runCatching {
            externalScheduleRepository.findBySchedule(scheduleId)
        }.getOrElse { throwable ->
            return failedAfterRecovery(
                recovery,
                "Failed to load current external schedule records: " +
                    (throwable.message ?: throwable::class.simpleName),
            )
        }

        val syncRecords = runCatching {
            syncPersistenceRepository.findBySchedule(scheduleId)
        }.getOrElse { throwable ->
            return failedAfterRecovery(
                recovery,
                "Failed to load sync history: " +
                    (throwable.message ?: throwable::class.simpleName),
            )
        }

        val reconciliation = runCatching {
            reconciliationService.reconcile(
                scheduleId = scheduleId,
                currentRecords = currentRecords,
                syncRecords = syncRecords,
            )
        }.getOrElse { throwable ->
            return failedAfterRecovery(
                recovery,
                "Reconciliation failed: " +
                    (throwable.message ?: throwable::class.simpleName),
            )
        }

        val warnings = buildList {
            addAll(recovery.warnings)
            addAll(reconciliation.warnings)

            if (recovery.status == ExternalScheduleSyncRunRecoveryStatus.EMPTY) {
                add(
                    "Run Journal was empty; reconciliation used current external records " +
                        "and durable sync history",
                )
            }

            if (recovery.status == ExternalScheduleSyncRunRecoveryStatus.PARTIAL) {
                add(
                    "Run recovery was partial; reconciliation was still evaluated from " +
                        "durable external state and sync history",
                )
            }
        }

        val status = when {
            reconciliation.errors.isNotEmpty() ->
                ExternalScheduleSyncRecoveryReconciliationStatus.FAILED

            recovery.status == ExternalScheduleSyncRunRecoveryStatus.EMPTY ->
                ExternalScheduleSyncRecoveryReconciliationStatus.RECOVERY_EMPTY

            recovery.status == ExternalScheduleSyncRunRecoveryStatus.PARTIAL ->
                ExternalScheduleSyncRecoveryReconciliationStatus.PARTIAL

            warnings.isNotEmpty() ->
                ExternalScheduleSyncRecoveryReconciliationStatus.RECONCILED_WITH_WARNINGS

            else ->
                ExternalScheduleSyncRecoveryReconciliationStatus.RECONCILED
        }

        return ExternalScheduleSyncRecoveryReconciliationResult(
            status = status,
            recovery = recovery,
            reconciliation = reconciliation,
            projection = recovery.projection,
            warnings = warnings,
            errors = recovery.errors + reconciliation.errors,
        )
    }

    private fun failedAfterRecovery(
        recovery: me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryResult,
        message: String,
    ) = ExternalScheduleSyncRecoveryReconciliationResult(
        status = ExternalScheduleSyncRecoveryReconciliationStatus.FAILED,
        recovery = recovery,
        projection = recovery.projection,
        warnings = recovery.warnings,
        errors = recovery.errors + message,
    )

    private fun failure(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        message: String,
    ) = ExternalScheduleSyncRecoveryReconciliationResult(
        status = ExternalScheduleSyncRecoveryReconciliationStatus.FAILED,
        recovery = ExternalScheduleSyncRunRecoveryResult(
            scheduleId = scheduleId,
            runId = runId,
            status = ExternalScheduleSyncRunRecoveryStatus.FAILED,
            projection = null,
            persistedRun = seed,
            restoredEventCount = 0,
            errors = listOf(message),
        ),
        errors = listOf(message),
    )
}
