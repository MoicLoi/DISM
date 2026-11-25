package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId

/**
 * 13.7
 *
 * 一条历史决策上的具体完整性问题。
 *
 * 每一种问题都带足够的信息让 App **不查数据库也能解释给用户听**：
 * 哪一条决策、缺什么、和什么矛盾、两边的值分别是什么。
 *
 * 前三种是"缺"（DEGRADED），后面是"矛盾"（BROKEN）。
 */
sealed class DynamicScheduleHistoryIntegrityIssue {

    abstract val itemId: DynamicScheduleItemId

    /**
     * Snapshot 指向的 Trace 不存在。
     */
    data class MissingTrace(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val traceId: DynamicScheduleDecisionTraceId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    /**
     * Snapshot 对应的 ReplayInput 不存在。
     *
     * 后果：这条决策**无法重放**，但决策本身仍然可读。
     */
    data class MissingReplayInput(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    /**
     * Snapshot 本身没有 traceId
     * （例如 12.15 之前的旧数据）。
     */
    data class SnapshotWithoutTrace(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    data class TraceItemMismatch(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val traceId: DynamicScheduleDecisionTraceId,

        val snapshotItemId: String,

        val traceItemId: String,
    ) : DynamicScheduleHistoryIntegrityIssue()

    data class ScheduleRevisionMismatch(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val traceId: DynamicScheduleDecisionTraceId,

        val snapshotRevision: Long,

        val traceRevision: Long,
    ) : DynamicScheduleHistoryIntegrityIssue()

    data class EngineVersionMismatch(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val traceId: DynamicScheduleDecisionTraceId,

        val snapshotEngineVersion: String,

        val traceEngineVersion: String,
    ) : DynamicScheduleHistoryIntegrityIssue()

    data class ReplayInputSnapshotMismatch(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val replaySnapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    data class ReplayInputRevisionMismatch(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val snapshotRevision: Long,

        val replayRevision: Long,
    ) : DynamicScheduleHistoryIntegrityIssue()

    data class ReplayInputEngineVersionMismatch(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val snapshotEngineVersion: String,

        val replayEngineVersion: String,
    ) : DynamicScheduleHistoryIntegrityIssue()

    data class ReplayInputItemMismatch(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,

        val replayItemId: String,
    ) : DynamicScheduleHistoryIntegrityIssue()

    // -----------------------------------------------------------------
    // 13.17：反方向引用
    //
    // 13.7 只检查 Snapshot → {Trace, ReplayInput}。
    // 13.17 补上反过来的一侧：
    //
    // ```text
    // ReplayInput → Snapshot 还在吗？
    // Provenance  → Snapshot 还在吗？
    // ```
    //
    // 否则 Snapshot 被压缩掉之后的孤儿记录永远不会被发现。
    // -----------------------------------------------------------------

    /**
     * 13.17：ReplayInput 指向的 Snapshot 已经不存在。
     *
     * 它已经无法再被重放——重放结果没有决策可以归属。
     */
    data class ReplayInputMissingSnapshot(
        override val itemId: DynamicScheduleItemId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    /**
     * 13.17：Provenance 指向的 Snapshot 已经不存在。
     *
     * 注意这**不一定**是错误——
     * 13.12.10 明确说 Provenance 不参与 Retention 级联删除，
     * 所以"Snapshot 被压缩掉、Provenance 留下"是设计意图。
     *
     * 它表达的是：这条来源链已经失去了它描述的那次决策。
     */
    data class ProvenanceMissingSnapshot(
        override val itemId: DynamicScheduleItemId,

        val provenanceId:
            me.moiclathy.dism.schedule.provenance
                .DynamicScheduleDecisionProvenanceId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    /**
     * 13.17：Provenance 存在，但它对应的 ReplayInput 没了。
     *
     * Provenance 明确表示"这个 Snapshot 曾经被重放过"，
     * 那么它当时用的输入就应该还在——
     * 否则这条来源链再也无法被完整重建。
     */
    data class ProvenanceMissingReplayInput(
        override val itemId: DynamicScheduleItemId,

        val provenanceId:
            me.moiclathy.dism.schedule.provenance
                .DynamicScheduleDecisionProvenanceId,

        val snapshotId: DynamicScheduleDecisionSnapshotId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    /**
     * 13.17：校验记录声称验证过一次重放，
     * 但重放本身的关键信息不完整。
     *
     * ### 为什么第一版几乎不会产生它（13.17.10）
     *
     * Kotlin data class 已经通过构造器保证了这些字段，
     * 所以正常 API 很难产生这种状态。
     *
     * 它主要用于**旧数据库 / 迁移 / 手工修改 / 未来版本兼容**。
     */
    data class VerificationWithoutReplay(
        override val itemId: DynamicScheduleItemId,

        val provenanceId:
            me.moiclathy.dism.schedule.provenance
                .DynamicScheduleDecisionProvenanceId,
    ) : DynamicScheduleHistoryIntegrityIssue()

    /**
     * 13.17：重放用的引擎与校验用的引擎不同。
     *
     * ### 第一版**刻意不产生**这个 issue（13.17.9）
     *
     * ```text
     * Replay with 13.15
     * Verify with 13.16
     * ```
     *
     * 这是**允许**的：校验本可以用更新的引擎重新跑一遍。
     * 把它当成"历史损坏"会把正常情况误判成故障。
     *
     * 模型保留，是为了将来真正需要时
     * （例如要求"校验必须针对同一次重放"）能表达这个事实。
     */
    data class VerificationEngineMismatch(
        override val itemId: DynamicScheduleItemId,

        val provenanceId:
            me.moiclathy.dism.schedule.provenance
                .DynamicScheduleDecisionProvenanceId,

        val replayEngineVersion: String,

        val verificationEngineVersion: String,
    ) : DynamicScheduleHistoryIntegrityIssue()
}
