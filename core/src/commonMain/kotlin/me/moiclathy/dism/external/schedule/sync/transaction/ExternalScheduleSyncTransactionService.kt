package me.moiclathy.dism.external.schedule.sync.transaction

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction

/**
 * Sync Run Transaction Boundary。
 */
interface ExternalScheduleSyncTransactionService {

    fun begin(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,

        startedAt:
            Instant,

        totalActions:
            Int,
    ):
        ExternalScheduleSyncRunRecord

    fun actionCompleted(
        runId:
            ExternalScheduleSyncRunId,

        updatedAt:
            Instant,
    ):
        ExternalScheduleSyncRunRecord?

    fun actionFailed(
        runId:
            ExternalScheduleSyncRunId,

        updatedAt:
            Instant,

        message:
            String,
    ):
        ExternalScheduleSyncRunRecord?

    fun complete(
        runId:
            ExternalScheduleSyncRunId,

        completedAt:
            Instant,
    ):
        ExternalScheduleSyncRunRecord?

    fun recover(
        runId:
            ExternalScheduleSyncRunId,

        actions:
            List<ExternalScheduleSyncRunAction>,
    ):
        ExternalScheduleSyncRunRecoveryResult
}
