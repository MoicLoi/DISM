package me.moiclathy.dism.external.schedule.sync.projection

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncCheckpointStatus
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalSnapshot
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunPhase
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunStatus

/**
 * 纯确定性 Journal -> Run Projection。
 * Journal 是事实，seed 仅提供计划总量等尚未进入 Journal 的运行元数据。
 */
class DeterministicExternalScheduleSyncRunProjector : ExternalScheduleSyncRunProjector {
    override fun project(
        seed: ExternalScheduleSyncRunRecord,
        snapshot: ExternalScheduleSyncJournalSnapshot,
        projectedAt: Instant,
    ): ExternalScheduleSyncRunProjectionResult {
        if (seed.runId != snapshot.runId) return ExternalScheduleSyncRunProjectionResult.failure("seed runId does not match snapshot runId")
        if (seed.scheduleId != snapshot.scheduleId) return ExternalScheduleSyncRunProjectionResult.failure("seed scheduleId does not match snapshot scheduleId")

        val checkpoints = snapshot.checkpoints
        val terminalSuccessful = checkpoints.count {
            it.status == ExternalScheduleSyncCheckpointStatus.COMPLETED ||
                it.status == ExternalScheduleSyncCheckpointStatus.SKIPPED
        }
        val completed = checkpoints.count { it.status == ExternalScheduleSyncCheckpointStatus.COMPLETED }
        val failed = checkpoints.count { it.status == ExternalScheduleSyncCheckpointStatus.FAILED }
        val skipped = checkpoints.count { it.status == ExternalScheduleSyncCheckpointStatus.SKIPPED }
        val pending = checkpoints.count { !it.terminal }
        val observedActions = checkpoints.size
        val totalActions = maxOf(seed.totalActions, observedActions)

        if (completed > totalActions) return ExternalScheduleSyncRunProjectionResult.failure("projected completedActions exceeds totalActions")
        if (failed > totalActions) return ExternalScheduleSyncRunProjectionResult.failure("projected failedActions exceeds totalActions")
        if (completed + failed > totalActions) return ExternalScheduleSyncRunProjectionResult.failure("projected completedActions + failedActions exceeds totalActions")

        val events = snapshot.orderedEvents
        val hasRunStarted = events.any { it.type == ExternalScheduleSyncJournalEventType.RUN_STARTED }
        val hasRunCompleted = events.any { it.type == ExternalScheduleSyncJournalEventType.RUN_COMPLETED }
        val hasRecoveryStarted = events.any { it.type == ExternalScheduleSyncJournalEventType.RECOVERY_STARTED }
        val hasRecoveryCompleted = events.any { it.type == ExternalScheduleSyncJournalEventType.RECOVERY_COMPLETED }
        val hasPlanning = events.any { it.type == ExternalScheduleSyncJournalEventType.ACTION_STARTED || it.type == ExternalScheduleSyncJournalEventType.RUN_STARTED }
        val hasExecuting = events.any {
            it.type == ExternalScheduleSyncJournalEventType.ACTION_STARTED ||
                it.type == ExternalScheduleSyncJournalEventType.EVENT_CREATED ||
                it.type == ExternalScheduleSyncJournalEventType.EVENT_UPDATED ||
                it.type == ExternalScheduleSyncJournalEventType.MAPPING_COMMITTED ||
                it.type == ExternalScheduleSyncJournalEventType.ACTION_COMPLETED ||
                it.type == ExternalScheduleSyncJournalEventType.ACTION_FAILED ||
                it.type == ExternalScheduleSyncJournalEventType.ACTION_SKIPPED
        }

        val status = when {
            hasRecoveryCompleted && pending == 0 && failed == 0 -> ExternalScheduleSyncRunStatus.RECOVERED
            hasRunCompleted && pending == 0 && failed == 0 -> {
                if (totalActions == 0) ExternalScheduleSyncRunStatus.NO_CHANGES else ExternalScheduleSyncRunStatus.COMPLETED
            }
            failed > 0 && pending == 0 -> ExternalScheduleSyncRunStatus.FAILED
            failed > 0 -> ExternalScheduleSyncRunStatus.PARTIAL
            !hasRunStarted && !hasRecoveryStarted -> seed.status
            totalActions == 0 && (hasRunStarted || hasRunCompleted) -> ExternalScheduleSyncRunStatus.NO_CHANGES
            else -> ExternalScheduleSyncRunStatus.IN_PROGRESS
        }

        val phase = when {
            hasRecoveryStarted && !hasRecoveryCompleted -> ExternalScheduleSyncRunPhase.RECOVERING
            hasRecoveryCompleted -> ExternalScheduleSyncRunPhase.COMPLETED
            hasRunCompleted -> ExternalScheduleSyncRunPhase.COMPLETED
            hasExecuting -> ExternalScheduleSyncRunPhase.EXECUTING
            hasPlanning -> ExternalScheduleSyncRunPhase.PLANNING
            else -> seed.phase
        }

        val completedAt = when {
            status == ExternalScheduleSyncRunStatus.COMPLETED ||
                status == ExternalScheduleSyncRunStatus.RECOVERED ||
                status == ExternalScheduleSyncRunStatus.NO_CHANGES ->
                events.lastOrNull {
                    it.type == ExternalScheduleSyncJournalEventType.RUN_COMPLETED ||
                        it.type == ExternalScheduleSyncJournalEventType.RECOVERY_COMPLETED
                }?.occurredAt ?: seed.completedAt
            else -> null
        }

        val message = events.lastOrNull { it.type == ExternalScheduleSyncJournalEventType.ACTION_FAILED }?.message
            ?: seed.message

        val record = ExternalScheduleSyncRunRecord(
            scheduleId = seed.scheduleId,
            runId = seed.runId,
            status = status,
            phase = phase,
            startedAt = events.firstOrNull {
                it.type == ExternalScheduleSyncJournalEventType.RUN_STARTED
            }?.occurredAt ?: seed.startedAt,
            updatedAt = maxOfInstant(seed.updatedAt, events.lastOrNull()?.occurredAt ?: seed.updatedAt),
            completedAt = completedAt,
            totalActions = totalActions,
            completedActions = completed,
            failedActions = failed,
            message = message,
        )

        val warnings = buildList {
            if (observedActions < seed.totalActions) add("Journal has not observed all planned actions yet")
            if (terminalSuccessful > completed) add("Skipped actions are terminal but are not counted as completedActions")
            if (hasRunCompleted && (pending > 0 || failed > 0)) add("RUN_COMPLETED exists but journal projection is not fully successful")
        }

        return ExternalScheduleSyncRunProjectionResult.success(
            ExternalScheduleSyncRunProjection(
                record = record,
                projectedAt = projectedAt,
                sourceScheduleId = snapshot.scheduleId,
                journalEventCount = events.size,
                checkpointCount = checkpoints.size,
                pendingActionCount = pending,
                skippedActionCount = skipped,
            ),
            warnings,
        )
    }

    private fun maxOfInstant(a: Instant, b: Instant): Instant = if (a >= b) a else b
}
