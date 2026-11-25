package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRepository

/**
 * 确定性的启动恢复协调器。
 * 不执行 Retry；只恢复 Journal → Projection → Run State。
 */
class DeterministicExternalScheduleSyncRunRecoveryCoordinator(
    private val journalRecoveryService: ExternalScheduleSyncJournalRecoveryService,
    private val runRepository: ExternalScheduleSyncRunRepository,
) : ExternalScheduleSyncRunRecoveryCoordinator {

    override fun recover(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncRunRecoveryResult {
        if (seed.scheduleId != scheduleId) {
            return failure(scheduleId, runId, "seed scheduleId does not match requested scheduleId")
        }
        if (seed.runId != runId) {
            return failure(scheduleId, runId, "seed runId does not match requested runId")
        }

        val recovery = journalRecoveryService.recover(
            scheduleId = scheduleId,
            runId = runId,
            seed = seed,
            projectedAt = projectedAt,
        )

        if (recovery.status == ExternalScheduleSyncJournalRecoveryStatus.EMPTY) {
            // 没有 Journal 时保留 seed，不伪造“恢复成功”。
            return ExternalScheduleSyncRunRecoveryResult(
                scheduleId = scheduleId,
                runId = runId,
                status = ExternalScheduleSyncRunRecoveryStatus.EMPTY,
                projection = null,
                persistedRun = seed,
                restoredEventCount = 0,
                warnings = recovery.warnings + "No persisted Journal events were found; seed Run State was retained",
                errors = recovery.errors,
            )
        }

        val projection = recovery.projection
            ?: return failure(
                scheduleId,
                runId,
                "Journal recovery returned no Run projection",
                recovery.warnings,
                recovery.errors,
            )

        return runCatching {
            runRepository.save(projection.record)
        }.fold(
            onSuccess = { saved ->
                val status = when (recovery.status) {
                    ExternalScheduleSyncJournalRecoveryStatus.RESTORED ->
                        ExternalScheduleSyncRunRecoveryStatus.PERSISTED
                    ExternalScheduleSyncJournalRecoveryStatus.PARTIAL ->
                        ExternalScheduleSyncRunRecoveryStatus.PARTIAL
                    else -> ExternalScheduleSyncRunRecoveryStatus.FAILED
                }

                ExternalScheduleSyncRunRecoveryResult(
                    scheduleId = scheduleId,
                    runId = runId,
                    status = status,
                    projection = projection,
                    persistedRun = saved,
                    restoredEventCount = recovery.restoredEventCount,
                    warnings = recovery.warnings,
                    errors = recovery.errors,
                )
            },
            onFailure = { throwable ->
                failure(
                    scheduleId,
                    runId,
                    "Failed to persist projected Run State: ${throwable.message ?: throwable::class.simpleName}",
                    recovery.warnings,
                    recovery.errors + "Run State persistence failed",
                )
            },
        )
    }

    private fun failure(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        message: String,
        warnings: List<String> = emptyList(),
        errors: List<String> = emptyList(),
    ) = ExternalScheduleSyncRunRecoveryResult(
        scheduleId = scheduleId,
        runId = runId,
        status = ExternalScheduleSyncRunRecoveryStatus.FAILED,
        warnings = warnings,
        errors = errors + message,
    )
}
