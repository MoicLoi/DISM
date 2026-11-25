package me.moiclathy.dism.external.schedule.sync.reconciliation

import me.moiclathy.dism.external.schedule.sync.projection.ExternalScheduleSyncRunProjection
import me.moiclathy.dism.external.schedule.sync.recovery.ExternalScheduleSyncRunRecoveryResult

/**
 * Result of the 11.28 Recovery -> Reconciliation boundary.
 *
 * This layer restores durable Run State and produces reconciliation decisions.
 * It does not execute CREATE/UPDATE/REMOVAL actions.
 */
data class ExternalScheduleSyncRecoveryReconciliationResult(
    val status: ExternalScheduleSyncRecoveryReconciliationStatus,
    val recovery: ExternalScheduleSyncRunRecoveryResult,
    val reconciliation: ExternalScheduleSyncReconciliationResult? = null,
    val projection: ExternalScheduleSyncRunProjection? = recovery.projection,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val successful: Boolean
        get() = status != ExternalScheduleSyncRecoveryReconciliationStatus.FAILED &&
            errors.isEmpty()

    val actionRequiredCount: Int
        get() = reconciliation?.actionRequiredCount ?: 0

    val consistentCount: Int
        get() = reconciliation?.consistentCount ?: 0

    val missingEventCount: Int
        get() = reconciliation?.missingEventCount ?: 0

    val incompleteCount: Int
        get() = reconciliation?.incompleteCount ?: 0

    val hasActions: Boolean
        get() = actionRequiredCount > 0
}
