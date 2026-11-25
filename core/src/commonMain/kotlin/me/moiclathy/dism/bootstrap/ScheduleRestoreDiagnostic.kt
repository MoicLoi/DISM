package me.moiclathy.dism.bootstrap

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

/**
 * 13.4
 *
 * Schedule 启动恢复过程中的诊断信息。
 *
 * 恢复失败不应该因为一条历史记录损坏，
 * 就导致整个 Runtime 无法启动。
 *
 * 因此 Recovery Path 采用：
 *
 * ```text
 * 可恢复   → 返回可用数据 + diagnostic
 * 不可恢复 → 尽可能跳过该记录，而不是污染整个 Runtime
 * ```
 *
 * 与设计稿的两处补充：
 *
 * 1. [SnapshotUnreadable] / [TraceUnreadable]
 *    设计稿的三种诊断只覆盖"找不到"，
 *    但 13.4.6 明确要求"历史记录损坏也不能阻止启动"。
 *    要真正做到这一点，读取出错（例如数据库里出现了
 *    当前代码不认识的 enum 值）必须有地方记录，
 *    否则就只能抛异常。
 *
 * 2. 世界状态（Entity / RelationState）不走这套诊断：
 *    它损坏时确实应该更严格处理，
 *    历史决策只是辅助恢复与解释的数据。
 */
sealed class ScheduleRestoreDiagnostic {

    /**
     * 该 item 从来没有过决策快照。
     *
     * 首次启动、或者新加入的 item 都属于这种情况，
     * 是**正常状态**，不是错误。
     */
    data class SnapshotNotFound(
        val itemId:
            DynamicScheduleItemId,
    ) : ScheduleRestoreDiagnostic()

    /**
     * Snapshot 存在，但 Snapshot 指向的 Trace 不存在。
     *
     * 典型原因：
     *
     * ```text
     * Snapshot 写成功
     * Trace 写失败 / 被清理
     * ```
     */
    data class TraceMissing(
        val itemId:
            DynamicScheduleItemId,

        val snapshotId:
            DynamicScheduleDecisionSnapshotId,

        val traceId:
            DynamicScheduleDecisionTraceId,
    ) : ScheduleRestoreDiagnostic()

    /**
     * Snapshot 本身就没有 traceId。
     *
     * 说明这次决策当时没有产生 Trace
     * （例如 12.15 之前的旧数据）。
     */
    data class SnapshotWithoutTrace(
        val itemId:
            DynamicScheduleItemId,

        val snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ) : ScheduleRestoreDiagnostic()

    /**
     * Snapshot 读不出来（数据损坏 / 版本不兼容）。
     *
     * 13.4.6：跳过这条记录，继续启动。
     */
    data class SnapshotUnreadable(
        val itemId:
            DynamicScheduleItemId,

        val reason: String,
    ) : ScheduleRestoreDiagnostic()

    /**
     * Trace 读不出来（数据损坏 / 版本不兼容）。
     *
     * 13.4.6：跳过这条记录，继续启动。
     */
    data class TraceUnreadable(
        val itemId:
            DynamicScheduleItemId,

        val traceId:
            DynamicScheduleDecisionTraceId,

        val reason: String,
    ) : ScheduleRestoreDiagnostic()
}
