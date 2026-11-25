package me.moiclathy.dism.external.schedule.sync.recovery

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.projection.ExternalScheduleSyncRunProjection
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

data class ExternalScheduleSyncRunRecoveryResult(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val status: ExternalScheduleSyncRunRecoveryStatus,
    val projection: ExternalScheduleSyncRunProjection? = null,
    val persistedRun: ExternalScheduleSyncRunRecord? = null,
    val restoredEventCount: Int = 0,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    init {
        require(restoredEventCount >= 0)
    }

    val successful: Boolean
        get() = status == ExternalScheduleSyncRunRecoveryStatus.RESTORED ||
            status == ExternalScheduleSyncRunRecoveryStatus.PERSISTED ||
            status == ExternalScheduleSyncRunRecoveryStatus.EMPTY

    val hasErrors: Boolean
        get() = errors.isNotEmpty()
}
