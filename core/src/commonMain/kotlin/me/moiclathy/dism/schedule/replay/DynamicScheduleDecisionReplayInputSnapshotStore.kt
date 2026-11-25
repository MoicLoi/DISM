package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 12.26A / 13.9
 *
 * Replay Input 的持久化边界。
 *
 * 注意：
 *
 * Replay Input 与普通 Decision Snapshot 分开存储。
 *
 * Decision Snapshot
 *      = 面向历史查询/解释
 *
 * Replay Input Snapshot
 *      = 面向确定性重放
 */
interface DynamicScheduleDecisionReplayInputSnapshotStore {

    suspend fun save(
        snapshot:
            DynamicScheduleDecisionReplayInputSnapshot,
    )

    suspend fun get(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionReplayInputSnapshot?

    /**
     * 13.9：枚举**所有**已存储的 ReplayInput 的父 Snapshot id。
     *
     * ### 为什么不是 `all(): List<ReplayInputSnapshot>`
     *
     * 设计稿 13.9.6 想加 `all(): List<DynamicScheduleDecisionReplayInputSnapshot>`，
     * 但那个方法在本项目的存储模型下**无法诚实实现**：
     *
     * ```text
     * 13.3 的设计：ReplayInput 表只存 snapshot_id + canonical JSON payload，
     *              decisionSnapshot 从 Snapshot 表读出来（不重复存一份）。
     * ```
     *
     * 而 orphan 的定义恰恰是"父 Snapshot 已经不存在"——
     * 也就是 `all()` 会对**唯一需要它的那批数据**返回 null。
     * 一个调用者以为完整、实际静默漏掉关键行的枚举方法，
     * 比没有这个方法危险得多。
     *
     * 所以这里只承诺系统真正能回答的事实：
     *
     * ```text
     * 有哪些 ReplayInput 存在（无论父 Snapshot 还在不在）
     * ```
     *
     * 需要完整对象时用 [get]；需要发现孤儿时用这个。
     */
    suspend fun allSnapshotIds():
        List<DynamicScheduleDecisionSnapshotId>

    suspend fun delete(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    )

    suspend fun clear()
}
