package me.moiclathy.dism.external.schedule.sync.execution

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.idempotency.ExternalScheduleSyncActionIdentity
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * One concrete recovery/reconciliation action to execute.
 *
 * `previous` is required for UPDATE and HANDLE_REMOVED_EVENT.
 * `current` is required for CREATE and UPDATE.
 * RECREATE_EVENT_MAPPING accepts either record when the handler can reconstruct
 * the missing mapping from the durable external state.
 */
data class ExternalScheduleSyncActionExecutionRequest(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val externalKey: String,
    val action: ExternalScheduleSyncAction,
    val previous: ExternalSchedulePersistenceRecord? = null,
    val current: ExternalSchedulePersistenceRecord? = null,
) {
    init {
        require(externalKey.isNotBlank())

        if (current != null) {
            require(current.externalScheduleId == scheduleId)
            require(current.externalKey == externalKey)
        }

        if (previous != null) {
            require(previous.externalScheduleId == scheduleId)
            require(previous.externalKey == externalKey)
        }
    }

    val identity: ExternalScheduleSyncActionIdentity
        get() = ExternalScheduleSyncActionIdentity(
            scheduleId = scheduleId,
            runId = runId,
            externalKey = externalKey,
            action = action,
        )
}
