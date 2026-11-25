package me.moiclathy.dism.external.schedule.sync.projection

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

/** Journal 重放后的 Run 投影。 */
data class ExternalScheduleSyncRunProjection(
    val record: ExternalScheduleSyncRunRecord,
    val projectedAt: Instant,
    val sourceScheduleId: ExternalScheduleId,
    val journalEventCount: Int,
    val checkpointCount: Int,
    val pendingActionCount: Int,
    val skippedActionCount: Int,
) {
    init {
        require(journalEventCount >= 0)
        require(checkpointCount >= 0)
        require(pendingActionCount >= 0)
        require(skippedActionCount >= 0)
    }
}
