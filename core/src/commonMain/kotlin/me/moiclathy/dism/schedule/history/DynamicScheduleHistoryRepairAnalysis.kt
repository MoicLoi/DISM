package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 13.9.12
 *
 * 修复分析结果。
 *
 * ### 为什么不是把孤儿塞进 IntegrityReport
 *
 * 13.7 的 [DynamicScheduleHistoryIntegrityReport] 检查的是：
 *
 * ```text
 * Snapshot → Trace
 * Snapshot → ReplayInput
 * ```
 *
 * 而孤儿是**反方向**的：
 *
 * ```text
 * ReplayInput → Snapshot（不存在）
 * ```
 *
 * 强行合并会让"完整性报告"同时承担两个方向的责任，
 * 也会让 13.7 的只读语义变得含糊。
 *
 * 所以这里单独一层。
 */
data class DynamicScheduleHistoryRepairAnalysis(
    val integrity:
        DynamicScheduleHistoryIntegrityReport,

    /**
     * 孤儿 ReplayInput 的父 Snapshot id。
     *
     * 注意这是**全局**的：孤儿与具体 item 无关，
     * 所以同一个列表可能出现在多个 item 的分析结果里。
     */
    val orphanReplayInputSnapshotIds:
        List<DynamicScheduleDecisionSnapshotId> =
        emptyList(),
) {

    val hasOrphans: Boolean
        get() = orphanReplayInputSnapshotIds.isNotEmpty()

    val hasIntegrityIssues: Boolean
        get() = integrity.issues.isNotEmpty()
}
