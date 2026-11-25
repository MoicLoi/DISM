package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

/**
 * 11.27 兼容修正版接口。
 *
 * ExternalScheduleSyncJournalRecoveryResult 与
 * ExternalScheduleSyncJournalRecoveryStatus
 * 仍由 11.26 的独立文件声明，这里不再重复声明。
 */
interface ExternalScheduleSyncJournalRecoveryService {
    fun recover(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncJournalRecoveryResult
}
