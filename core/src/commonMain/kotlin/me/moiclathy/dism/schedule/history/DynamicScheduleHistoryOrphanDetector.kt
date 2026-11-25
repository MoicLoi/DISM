package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore

/**
 * 13.9.9
 *
 * 孤儿 ReplayInput 检测。
 *
 * ```text
 * ReplayInput
 *    ↓
 * Snapshot 不存在
 *    ↓
 * ReplayInput 已失去父对象  → 孤儿，可以安全清理
 * ```
 *
 * 注意这和下面这件事**完全不同**：
 *
 * ```text
 * Snapshot
 *    ↓
 * Trace 不存在              → 历史证据缺失，必须保留
 * ```
 *
 * 区别在于：
 *
 * - `Snapshot` 是**原始历史证据**，它缺了什么都不能删它；
 * - `ReplayInput` 是**重放能力产物**，没有父 Snapshot 时
 *   它什么也证明不了，留着只会让存储慢慢变脏。
 *
 * 检测是**全局**的（跨 item）：孤儿本身与哪个 item 无关。
 */
class DynamicScheduleHistoryOrphanDetector(
    private val snapshotStore:
        DynamicScheduleDecisionSnapshotStore,

    private val replayInputStore:
        DynamicScheduleDecisionReplayInputSnapshotStore,
) {

    suspend fun findOrphanReplayInputs():
        List<DynamicScheduleDecisionSnapshotId> =
        replayInputStore
            .allSnapshotIds()
            .filter { snapshotId ->
                snapshotStore.get(snapshotId) == null
            }
}
