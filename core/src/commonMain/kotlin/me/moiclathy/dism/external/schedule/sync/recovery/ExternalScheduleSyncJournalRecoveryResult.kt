package me.moiclathy.dism.external.schedule.sync.recovery

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalSnapshot
import me.moiclathy.dism.external.schedule.sync.projection.ExternalScheduleSyncRunProjection
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

data class ExternalScheduleSyncJournalRecoveryResult(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val status: ExternalScheduleSyncJournalRecoveryStatus,
    val snapshot: ExternalScheduleSyncJournalSnapshot?,
    val projection: ExternalScheduleSyncRunProjection?,
    val restoredEventCount: Int,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    val successful: Boolean
        get() = status == ExternalScheduleSyncJournalRecoveryStatus.RESTORED ||
            status == ExternalScheduleSyncJournalRecoveryStatus.EMPTY

    val hasErrors: Boolean
        get() = errors.isNotEmpty()
}
