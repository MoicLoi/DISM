package me.moiclathy.dism.external.schedule.sync.projection

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalSnapshot
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

/** 将 Journal Snapshot 确定性地投影为 Run State。 */
interface ExternalScheduleSyncRunProjector {
    fun project(
        seed: ExternalScheduleSyncRunRecord,
        snapshot: ExternalScheduleSyncJournalSnapshot,
        projectedAt: Instant,
    ): ExternalScheduleSyncRunProjectionResult
}
