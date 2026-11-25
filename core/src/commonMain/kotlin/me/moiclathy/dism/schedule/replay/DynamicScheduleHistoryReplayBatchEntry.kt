package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 13.14.2
 *
 * 批量重放里的一条结果。
 *
 * 用 sealed 而不是"成功列表 + 异常"：
 * 一项坏数据不应该让整个批次的结果变得不可用。
 */
sealed class DynamicScheduleHistoryReplayBatchEntry {

    abstract val snapshotId:
        DynamicScheduleDecisionSnapshotId

    data class Success(
        override val snapshotId:
            DynamicScheduleDecisionSnapshotId,

        val replay:
            DynamicScheduleHistoricalRuntimeReplayResult,

        val verification:
            DynamicScheduleReplayVerificationReport? = null,
    ) : DynamicScheduleHistoryReplayBatchEntry()

    data class Failure(
        override val snapshotId:
            DynamicScheduleDecisionSnapshotId,

        val message: String,
    ) : DynamicScheduleHistoryReplayBatchEntry()
}
