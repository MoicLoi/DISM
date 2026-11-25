package me.moiclathy.dism.schedule.provenance

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 13.12.6
 *
 * Provenance 的持久化边界。
 *
 * ### 为什么没有外键（13.12.10）
 *
 * ```text
 * Snapshot / Trace / ReplayInput / Provenance
 * ```
 *
 * 生命周期不同。
 *
 * [me.moiclathy.dism.schedule.history.DynamicScheduleHistoryRetentionService]
 * 会删除旧 Snapshot，但 Provenance **不应该被级联删除**——
 * 它本身就是"历史解释索引"，
 * 恰好是 Snapshot 消失之后唯一还能说明"这里曾经发生过什么"的东西。
 *
 * 所以这里保持**应用层引用**，不用 SQLite 外键。
 */
interface DynamicScheduleDecisionProvenanceStore {

    suspend fun save(
        provenance:
            DynamicScheduleDecisionProvenance,
    )

    suspend fun get(
        id:
            DynamicScheduleDecisionProvenanceId,
    ): DynamicScheduleDecisionProvenance?

    /**
     * 某条 Snapshot 最近一次重放记录。
     *
     * 排序确定性：`createdAt` 降序，`provenanceId` 降序。
     *
     * 同一个 Snapshot 被重放多次是正常的（每次一个 Provenance），
     * 而 `createdAt` 在确定性重放下可能相同
     * （它取自历史求值时间），所以必须有一个稳定的二级排序，
     * 否则"最近一次"会在不同进程间飘。
     */
    suspend fun latestForSnapshot(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionProvenance?

    /**
     * 13.17：某条 Snapshot 的**全部**重放记录。
     *
     * [latestForSnapshot] 只能看到最近一条。
     * 如果历史是：
     *
     * ```text
     * Snapshot A
     *  ├── Replay 1  损坏
     *  ├── Replay 2  正常
     *  └── Replay 3  正常
     * ```
     *
     * 只看 latest 的完整性检查会得出"A 是健康的"这个**错误结论**。
     * 所以完整性检查必须能看到全部。
     */
    suspend fun queryForSnapshot(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): List<DynamicScheduleDecisionProvenance>

    /**
     * 某个 item 的完整来源链时间线。
     */
    suspend fun queryForItem(
        itemId:
            DynamicScheduleItemId,
    ): List<DynamicScheduleDecisionProvenance>

    /**
     * 13.17：枚举**全部** provenance 记录。
     *
     * 完整性检查需要反方向遍历：
     *
     * ```text
     * Provenance → 它引用的 Snapshot 还在吗？
     * ```
     *
     * Snapshot 可能已经被压缩掉，
     * 于是再也无法通过"遍历 Snapshot"发现这条孤儿记录。
     *
     * 与 13.9 里 ReplayInput 的情况不同：provenance 记录是**自洽**的，
     * 不需要父 Snapshot 就能完整构造，所以这个枚举可以诚实实现。
     */
    suspend fun all():
        List<DynamicScheduleDecisionProvenance>

    /**
     * 13.12.14 / 13.16：Verification 晚于 Replay，
     * 因此需要单独回填这次的校验事实，而不是重写整条记录。
     *
     * 13.16 从"只回填一个状态字符串"升级成结构化记录——
     * `verifiedAt` / `differenceCount` / `diagnostics` 同样重要，
     * 而且它们和状态必须一起写，不能分行更新。
     */
    suspend fun updateVerification(
        id:
            DynamicScheduleDecisionProvenanceId,

        verification:
            DynamicScheduleReplayVerificationProvenance,
    )

    suspend fun clear()
}
