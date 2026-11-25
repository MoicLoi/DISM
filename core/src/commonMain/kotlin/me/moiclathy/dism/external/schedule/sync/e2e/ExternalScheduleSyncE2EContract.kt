package me.moiclathy.dism.external.schedule.sync.e2e

import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction

/**
 * Structural contract for 11.31.
 *
 * The contract intentionally checks boundaries rather than asserting concrete
 * Event internals. That keeps this stage compatible with the real Event API.
 */
object ExternalScheduleSyncE2EContract {
    private val requiredPhases = listOf(
        ExternalScheduleSyncE2EPhase.RECOVERY,
        ExternalScheduleSyncE2EPhase.RECONCILIATION,
        ExternalScheduleSyncE2EPhase.ACTION_MAPPING,
        ExternalScheduleSyncE2EPhase.IDEMPOTENCY,
        ExternalScheduleSyncE2EPhase.EVENT_ACTION,
        ExternalScheduleSyncE2EPhase.OBSERVATION,
        ExternalScheduleSyncE2EPhase.FINAL,
    )

    fun validate(trace: ExternalScheduleSyncE2ETrace): List<String> = buildList {
        if (trace.phases != requiredPhases) {
            add("11.31 phase sequence is incomplete or out of order")
        }

        if (trace.actions.zipWithNext().any { (a, b) -> a.externalKey > b.externalKey }) {
            add("Action traces are not deterministic by externalKey")
        }

        for (action in trace.actions) {
            when (action.reconciliationAction) {
                ExternalScheduleSyncReconciliationAction.NO_OP -> {
                    if (action.syncAction != null || action.executionStatus != null) {
                        add("NO_OP entered the action executor for ${action.externalKey}")
                    }
                }

                ExternalScheduleSyncReconciliationAction.COMPLETE_SYNC -> {
                    if (action.syncAction != null) {
                        add("COMPLETE_SYNC was incorrectly mapped to an Event action for ${action.externalKey}")
                    }
                    if (!action.completeSyncHandled && !action.failed) {
                        add("COMPLETE_SYNC was neither handled nor failed for ${action.externalKey}")
                    }
                }

                ExternalScheduleSyncReconciliationAction.RETRY_CREATE -> {
                    if (action.syncAction != ExternalScheduleSyncAction.CREATE_EVENT) {
                        add("RETRY_CREATE did not map to CREATE_EVENT for ${action.externalKey}")
                    }
                }

                ExternalScheduleSyncReconciliationAction.RETRY_UPDATE -> {
                    if (action.syncAction != ExternalScheduleSyncAction.UPDATE_EVENT) {
                        add("RETRY_UPDATE did not map to UPDATE_EVENT for ${action.externalKey}")
                    }
                }

                ExternalScheduleSyncReconciliationAction.RETRY_REMOVAL -> {
                    if (action.syncAction != ExternalScheduleSyncAction.HANDLE_REMOVED_EVENT) {
                        add("RETRY_REMOVAL did not map to HANDLE_REMOVED_EVENT for ${action.externalKey}")
                    }
                }

                ExternalScheduleSyncReconciliationAction.RECREATE_EVENT_MAPPING -> {
                    if (action.syncAction != ExternalScheduleSyncAction.RECREATE_EVENT_MAPPING) {
                        add("RECREATE_EVENT_MAPPING did not map to the Event mapping action for ${action.externalKey}")
                    }
                }
            }
        }
    }
}
