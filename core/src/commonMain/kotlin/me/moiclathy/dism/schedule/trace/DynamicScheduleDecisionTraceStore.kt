package me.moiclathy.dism.schedule.trace

/**
 * 12.15 / 13.3
 *
 * Decision Trace 的持久化 **Port**。
 *
 * 13.3 的一处收口：
 *
 * Trace 不需要 `findLatestByItemId()`。
 *
 * 因为关系已经由 Snapshot 表达出来了：
 *
 * ```text
 * Snapshot ──traceId──► Trace
 * ```
 *
 * 启动恢复路径是
 * `snapshotStore.findLatestByItemId(itemId)` → `traceStore.get(traceId)`，
 * 而不是再给 Trace 加一套"按 item 找最近"的索引。
 *
 * `query()` 保留给显式历史查询（`historyTraces()`）。
 */
interface DynamicScheduleDecisionTraceStore {

    suspend fun save(
        record: DynamicScheduleDecisionTraceRecord,
    )

    suspend fun get(
        traceId: DynamicScheduleDecisionTraceId,
    ): DynamicScheduleDecisionTraceRecord?

    suspend fun delete(
        traceId: DynamicScheduleDecisionTraceId,
    )

    suspend fun clear()

    suspend fun query(
        query: DynamicScheduleDecisionTraceQuery,
    ): DynamicScheduleDecisionTraceQueryResult
}
