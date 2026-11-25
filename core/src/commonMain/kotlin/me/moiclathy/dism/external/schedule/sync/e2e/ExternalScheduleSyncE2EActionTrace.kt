package me.moiclathy.dism.external.schedule.sync.e2e

import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionStatus
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction

/**
 * One reconciliation decision traced through the 11.30 action boundary.
 *
 * COMPLETE_SYNC is intentionally represented without an Event action because
 * it only repairs Sync history after an Event already exists.
 */
data class ExternalScheduleSyncE2EActionTrace(
    val externalKey: String,
    val reconciliationAction: ExternalScheduleSyncReconciliationAction,
    val syncAction: ExternalScheduleSyncAction? = null,
    val executionStatus: ExternalScheduleSyncActionExecutionStatus? = null,
    val eventId: String? = null,
    val completeSyncHandled: Boolean = false,
    val message: String? = null,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    init {
        require(externalKey.isNotBlank()) { "externalKey must not be blank" }

        if (reconciliationAction == ExternalScheduleSyncReconciliationAction.COMPLETE_SYNC) {
            require(syncAction == null) {
                "COMPLETE_SYNC must not be mapped to an Event action"
            }
        }

        if (reconciliationAction == ExternalScheduleSyncReconciliationAction.NO_OP) {
            require(syncAction == null) {
                "NO_OP must not be mapped to an Event action"
            }
            require(executionStatus == null) {
                "NO_OP must not reach the Event action executor"
            }
        }

        if (executionStatus != null) {
            require(syncAction != null) {
                "executionStatus requires syncAction"
            }
        }
    }

    val executed: Boolean
        get() = executionStatus == ExternalScheduleSyncActionExecutionStatus.EXECUTED

    val skipped: Boolean
        get() = executionStatus == ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_IN_PROGRESS ||
            executionStatus == ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_COMPLETED

    val failed: Boolean
        get() = executionStatus == ExternalScheduleSyncActionExecutionStatus.FAILED ||
            executionStatus == ExternalScheduleSyncActionExecutionStatus.REJECTED ||
            errors.isNotEmpty()

    val successful: Boolean
        get() = !failed && (
            reconciliationAction == ExternalScheduleSyncReconciliationAction.NO_OP ||
                completeSyncHandled ||
                executed ||
                skipped
            )
}
