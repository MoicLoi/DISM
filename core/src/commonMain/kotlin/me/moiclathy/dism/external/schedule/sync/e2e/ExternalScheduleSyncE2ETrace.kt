package me.moiclathy.dism.external.schedule.sync.e2e

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncRecoveryReconciliationResult
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Complete execution trace for one 11.31 run.
 */
data class ExternalScheduleSyncE2ETrace(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val reconciliation: ExternalScheduleSyncRecoveryReconciliationResult,
    val actions: List<ExternalScheduleSyncE2EActionTrace>,
    val phases: List<ExternalScheduleSyncE2EPhase>,
) {
    init {
        require(actions.zipWithNext().all { (a, b) -> a.externalKey <= b.externalKey }) {
            "actions must be sorted by externalKey"
        }
        require(phases.distinct().size == phases.size) {
            "phases must not contain duplicates"
        }
    }

    val actionCount: Int
        get() = actions.size

    val executedCount: Int
        get() = actions.count { it.executed }

    val skippedCount: Int
        get() = actions.count { it.skipped }

    val failedCount: Int
        get() = actions.count { it.failed }

    val completeSyncCount: Int
        get() = actions.count { it.completeSyncHandled }

    val noOpCount: Int
        get() = actions.count {
            it.reconciliationAction ==
                me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction.NO_OP
        }
}
