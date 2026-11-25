package me.moiclathy.dism.external.schedule.sync.transaction

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * Run-level Recovery Plan。
 */
data class ExternalScheduleSyncRunRecoveryPlan(

    val scheduleId:
        ExternalScheduleId,

    val runId:
        ExternalScheduleSyncRunId,

    val originalRun:
        ExternalScheduleSyncRunRecord,

    val pendingActions:
        List<ExternalScheduleSyncRunAction>,

    val failedActions:
        List<ExternalScheduleSyncRunAction>,

    val completedActions:
        List<ExternalScheduleSyncRunAction>,
) {

    val recoverableActions:
        List<ExternalScheduleSyncRunAction>
        get() =
            pendingActions +
                failedActions

    val recoverableCount: Int
        get() =
            recoverableActions.size

    val hasWork: Boolean
        get() =
            recoverableActions.isNotEmpty()
}
