package me.moiclathy.dism.external.schedule.sync.replay

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

data class ExternalScheduleSyncJournalReplayRequest(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val seed: ExternalScheduleSyncRunRecord,
    val projectedAt: Instant,
    val mode: ExternalScheduleSyncJournalReplayMode = ExternalScheduleSyncJournalReplayMode.FULL_REPLAY,
) {
    init {
        require(seed.scheduleId == scheduleId) { "seed scheduleId does not match request" }
        require(seed.runId == runId) { "seed runId does not match request" }
    }
}
