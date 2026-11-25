package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

/**
 * 应用启动时恢复一个 Sync Run 的边界。
 *
 * Journal recovery 负责把持久化事实恢复成 Projection；本 Coordinator 只负责
 * 将 Projection 的当前 Run Record 写回 Run Repository。
 */
interface ExternalScheduleSyncRunRecoveryCoordinator {
    fun recover(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncRunRecoveryResult
}
