package me.moiclathy.dism.bootstrap

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 从持久化层恢复出的最近一次调度决策。
 *
 * 与 [RestoredCoreState] 分开：
 *
 * - RestoredCoreState 是 5.x 世界状态；
 * - RestoredScheduleState 是 12.x 决策历史。
 *
 * 13.4 的核心约束：
 *
 * > **Snapshot 与 Trace 必须属于同一次决策。**
 *
 * Trace 不再按 item "取最新"（那可能取到另一次决策的 Trace），
 * 而是通过 `Snapshot.traceId` 精确关联：
 *
 * ```text
 * snapshotFor(item)
 *       │
 *       ▼ traceId
 * traceFor(item)
 * ```
 *
 * 恢复过程永远不会抛异常：
 * 拿不到的东西会被记到 [diagnostics]，
 * 让 App 可以"显示 Snapshot + 提示历史解释链暂不可用"，
 * 而不是整个 DISM 启动失败。
 */
data class RestoredScheduleState(
    val snapshots:
        List<DynamicScheduleDecisionSnapshot> =
        emptyList(),

    val traces:
        List<DynamicScheduleDecisionTraceRecord> =
        emptyList(),

    val diagnostics:
        List<ScheduleRestoreDiagnostic> =
        emptyList(),
) {

    val isEmpty: Boolean
        get() =
            snapshots.isEmpty() &&
                traces.isEmpty()

    val hasDiagnostics: Boolean
        get() =
            diagnostics.isNotEmpty()

    /**
     * 通过 itemId 获取最近 Snapshot。
     */
    fun snapshotFor(
        itemId:
            DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshot? =
        snapshots.firstOrNull {
            it.itemId == itemId
        }

    /**
     * 获取与 Snapshot **一一对应**的 Trace。
     *
     * 不再使用"该 item 最新 Trace"的弱关联。
     */
    fun traceFor(
        itemId:
            DynamicScheduleItemId,
    ): DynamicScheduleDecisionTraceRecord? {

        val snapshot =
            snapshotFor(itemId)
                ?: return null

        val traceId =
            snapshot.traceId
                ?: return null

        return traces.firstOrNull {
            it.trace.id == traceId
        }
    }

    /**
     * 该 item 在本次恢复中产生的诊断。
     *
     * App 可以用它区分：
     *
     * ```text
     * 没有历史        → SnapshotNotFound
     * 有历史但缺解释链 → TraceMissing / SnapshotWithoutTrace
     * 历史损坏        → SnapshotUnreadable / TraceUnreadable
     * ```
     */
    fun diagnosticsFor(
        itemId:
            DynamicScheduleItemId,
    ): List<ScheduleRestoreDiagnostic> =
        diagnostics.filter { diagnostic ->
            when (diagnostic) {

                is ScheduleRestoreDiagnostic
                    .SnapshotNotFound ->
                    diagnostic.itemId == itemId

                is ScheduleRestoreDiagnostic
                    .TraceMissing ->
                    diagnostic.itemId == itemId

                is ScheduleRestoreDiagnostic
                    .SnapshotWithoutTrace ->
                    diagnostic.itemId == itemId

                is ScheduleRestoreDiagnostic
                    .SnapshotUnreadable ->
                    diagnostic.itemId == itemId

                is ScheduleRestoreDiagnostic
                    .TraceUnreadable ->
                    diagnostic.itemId == itemId
            }
        }
}
