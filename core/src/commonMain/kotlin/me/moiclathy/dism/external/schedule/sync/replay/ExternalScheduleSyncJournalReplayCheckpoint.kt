package me.moiclathy.dism.external.schedule.sync.replay

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncCheckpointStatus

data class ExternalScheduleSyncJournalReplayCheckpoint(
    val externalKey: String,
    val action: ExternalScheduleSyncAction,
    val status: ExternalScheduleSyncCheckpointStatus,
    val eventId: String? = null,
    val startedAt: Instant? = null,
    val lastUpdatedAt: Instant? = null,
    val message: String? = null,
) {
    init {
        require(externalKey.isNotBlank())
    }
}
