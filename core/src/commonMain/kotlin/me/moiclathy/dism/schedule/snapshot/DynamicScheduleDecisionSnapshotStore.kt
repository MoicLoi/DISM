package me.moiclathy.dism.schedule.snapshot

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 12.15 / 13.3
 *
 * Decision Snapshot 的持久化 **Port**。
 *
 * 它表达的是：
 *
 * > "我需要保存/读取决策历史。"
 *
 * 而不是：
 *
 * > "我要操作 SQLDelight。"
 *
 * 因此实现可以是 InMemory，也可以是 SQLDelight，
 * 而 Core 完全不知道区别。
 */
interface DynamicScheduleDecisionSnapshotStore {

    suspend fun save(
        snapshot: DynamicScheduleDecisionSnapshot,
    )

    suspend fun get(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionSnapshot?

    /**
     * 只读最近一次。
     *
     * 13.3：启动恢复路径必须走这个端口，
     * 而不是 "全量加载 + maxBy"。
     *
     * SQL 实现对应 `ORDER BY captured_at DESC LIMIT 1`。
     */
    suspend fun findLatestByItemId(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshot?

    /**
     * 16.32：一批条目的"最近一次"，**一次往返**。
     *
     * ### 为什么需要它
     *
     * 展示层要判断"决策变了没有"，依据是"现在算出来的"
     * 与"已记录下来的"逐条比对。只有 [findLatestByItemId] 时
     * 这会变成 N+1：
     *
     * ```text
     * 200 个条目  →  200 次查询
     * ```
     *
     * 实测（`LatestDecisionQueryCostTest`，真实 SQLDelight）：
     *
     * ```text
     * 200 次单条查询      81 ms
     * 一次完整评估         1 ms
     * ```
     *
     * 也就是说这个 N+1 比它守护的那条评估链**贵约 80 倍**，
     * 而且每次启动都会跑。
     *
     * ### 契约
     *
     * 每个 itemId **至多一条**，且与 [findLatestByItemId]
     * 选出的完全相同（时间戳并列时取较新的 snapshotId）。
     *
     * 从未记录过的条目不出现——调用方据此判断"没有历史"。
     */
    suspend fun findLatestByItemIds(
        itemIds: Collection<DynamicScheduleItemId>,
    ): List<DynamicScheduleDecisionSnapshot>

    suspend fun delete(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    )

    suspend fun clear()

    /**
     * 历史查询走独立端口：
     *
     * ```text
     * Startup Path  ── findLatestByItemId()   （只读一行）
     * History Path  ── query()                （显式请求）
     * ```
     */
    suspend fun query(
        query: DynamicScheduleDecisionSnapshotQuery,
    ): DynamicScheduleDecisionSnapshotQueryResult
}
