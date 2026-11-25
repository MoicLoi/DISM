package me.moiclathy.dism.external.schedule.sync.replay

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalSnapshot
import me.moiclathy.dism.external.schedule.sync.projection.ExternalScheduleSyncRunProjection
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

data class ExternalScheduleSyncJournalReplayResult(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val status: ExternalScheduleSyncJournalReplayStatus,
    val snapshot: ExternalScheduleSyncJournalSnapshot? = null,
    val projection: ExternalScheduleSyncRunProjection? = null,
    val checkpoints: List<ExternalScheduleSyncJournalReplayCheckpoint> = emptyList(),
    val replayedEventCount: Int = 0,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    init {
        require(replayedEventCount >= 0)
    }

    val successful: Boolean
        get() = status == ExternalScheduleSyncJournalReplayStatus.REPLAYED ||
            status == ExternalScheduleSyncJournalReplayStatus.EMPTY

    val hasEvents: Boolean
        get() = replayedEventCount > 0
}
