package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

/**
 * 13.8
 *
 * 面对一条不完整的历史，**允许**采取的动作。
 *
 * 注意这里故意**没有**：
 *
 * ```text
 * RebuildTrace(...)
 * RewriteSnapshot(...)
 * RewriteReplayInput(...)
 * ```
 *
 * 因为它们都可能破坏历史真实性：
 *
 * ```text
 * Snapshot 缺 Trace
 *      ↓
 * 重新生成一个 Trace —— 但那不是原始历史
 *
 * Snapshot 缺 ReplayInput
 *      ↓
 * Snapshot 并没有保存 input / previousState / policy，
 * 所以根本恢复不出真正的 ReplayInput
 *
 * Snapshot revision = 10，Trace revision = 11
 *      ↓
 * 系统无法知道哪一侧是对的 —— 覆盖任何一侧都是篡改证据
 * ```
 *
 * 因此现阶段唯一允许的"动作"是 [DeleteOrphanReplayInput]，
 * 而且它连"发现"都还做不到（见 [DynamicScheduleHistoryRepairPlanner] 的说明）。
 *
 * 与设计稿的一处偏差：这里保留**强类型 id**，
 * 而不是 `String`。设计稿自己在构造时做了一次
 * `issue.snapshotId.value.toString()`，读回来又要重新解析；
 * 计划本身就是给上层用的，没有理由把类型信息丢掉。
 */
sealed class DynamicScheduleHistoryRepairAction {

    /**
     * 不需要任何处理。
     */
    data object NoOp :
        DynamicScheduleHistoryRepairAction()

    /**
     * 可以安全删除一个已经失去父级 Snapshot 的 ReplayInput。
     *
     * 这是唯一"不改变任何现存历史对象语义"的操作，
     * 所以未来它会被允许自动执行。
     *
     * 但当前 Store 端口只提供 `get(id) / save / delete(id) / clear`，
     * **没有 `all()` / `query()`**，所以现在还无法可靠枚举
     * orphan ReplayInput。13.8 不假装能做到这件事——
     * 等历史索引阶段补上枚举能力后，这里才会真正被产出。
     */
    data class DeleteOrphanReplayInput(
        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryRepairAction()

    /**
     * 缺少 Trace。不能自动重建。
     */
    data class PreserveMissingTrace(
        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val traceId: DynamicScheduleDecisionTraceId,
    ) : DynamicScheduleHistoryRepairAction()

    /**
     * 缺少 ReplayInput。当前 Snapshot 提供不了完整 replay input。
     */
    data class PreserveMissingReplayInput(
        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryRepairAction()

    /**
     * Snapshot 没有 Trace 引用（旧数据）。
     */
    data class PreserveSnapshotWithoutTrace(
        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryRepairAction()

    /**
     * 发现元数据矛盾。不允许自动覆盖任何一侧。
     *
     * [description] 里带上双方的实际取值，
     * 这样 App / 诊断界面不需要回头再查一次数据库
     * 就能告诉用户到底哪里不一致。
     */
    data class PreserveConflict(
        /**
         * 若不是发生在某条 Snapshot 上的冲突则为 null。
         *
         * 例如 13.17 的 `VerificationWithoutReplay`
         * 是按 **provenance** 定位的，没有 snapshotId。
         */
        val snapshotId: DynamicScheduleDecisionSnapshotId?,

        val description: String,
    ) : DynamicScheduleHistoryRepairAction()
}
