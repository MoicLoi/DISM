package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 12.27
 *
 * True Historical Runtime Replay 的入口参数。
 *
 * 注意：
 *
 * snapshotId 不是当前状态的 ID。
 * 它指向某一次已经保存下来的历史 Runtime Input。
 */
data class DynamicScheduleHistoricalRuntimeReplayInput(
    val snapshotId: DynamicScheduleDecisionSnapshotId,
)
