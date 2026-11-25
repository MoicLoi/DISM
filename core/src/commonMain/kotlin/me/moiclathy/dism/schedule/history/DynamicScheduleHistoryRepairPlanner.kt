package me.moiclathy.dism.schedule.history

/**
 * 13.8
 *
 * Repair Plan 生成器。
 *
 * > **只有"不会改变任何现存历史对象语义"的操作才允许成为动作。**
 *
 * 映射规则很直接，但每一条都对应一个"为什么不能自动修"的理由：
 *
 * | 问题 | 计划 | 为什么不能自动修 |
 * | --- | --- | --- |
 * | 缺 Trace | Preserve | 原 Trace 内容无从得知 |
 * | 无 traceId | Preserve | 旧数据本来就没有 |
 * | 缺 ReplayInput | Preserve | Snapshot 没有保存 input/state/policy |
 * | 一切元数据矛盾 | Preserve | 不知道哪一侧才是对的 |
 *
 * 这个类**没有任何 Store 依赖**，因此它在结构上就不可能修改历史。
 */
class DynamicScheduleHistoryRepairPlanner {

    /**
     * 13.8 的入口：只有完整性报告。
     *
     * 这种情况下**不可能**产出 [DynamicScheduleHistoryRepairAction.DeleteOrphanReplayInput]，
     * 因为孤儿是反方向引用（ReplayInput → Snapshot），
     * 完整性报告看不到它（13.9.12）。
     */
    fun plan(
        report: DynamicScheduleHistoryIntegrityReport,
    ): DynamicScheduleHistoryRepairPlan =
        plan(
            DynamicScheduleHistoryRepairAnalysis(
                integrity = report,
            ),
        )

    /**
     * 13.9 的入口：完整性 + 孤儿分析。
     */
    fun plan(
        analysis: DynamicScheduleHistoryRepairAnalysis,
    ): DynamicScheduleHistoryRepairPlan {

        val report = analysis.integrity

        /*
         * 13.17：孤儿 ReplayInput 现在有**两个**来源：
         *
         * 1. 完整性报告里的 `ReplayInputMissingSnapshot`
         *    （13.17 给 per-item 检查补的反向遍历）；
         * 2. 分析器给出的全局孤儿列表（13.9）。
         *
         * 两者会重叠，所以先按 snapshotId 取并集，
         * 保证一个孤儿只产生一条清理动作。
         */
        val orphanSnapshotIds =
            buildSet {

                report.issues.forEach { issue ->

                    if (
                        issue is
                        DynamicScheduleHistoryIntegrityIssue
                            .ReplayInputMissingSnapshot
                    ) {
                        add(issue.snapshotId)
                    }
                }

                addAll(
                    analysis.orphanReplayInputSnapshotIds,
                )
            }

        val actions =
            buildList {

                report.issues.forEach { issue ->

                    /*
                     * 孤儿由上面的并集统一产出，
                     * 这里跳过以免重复。
                     */
                    if (
                        issue is
                        DynamicScheduleHistoryIntegrityIssue
                            .ReplayInputMissingSnapshot
                    ) {
                        return@forEach
                    }

                    add(actionFor(issue))
                }

                orphanSnapshotIds
                    .sortedBy { it.value.toString() }
                    .forEach { snapshotId ->
                        add(
                            DynamicScheduleHistoryRepairAction
                                .DeleteOrphanReplayInput(
                                    snapshotId = snapshotId,
                                ),
                        )
                    }
            }

        /*
         * 13.17：全库检查报告（itemId == null）**不能**转成修复计划。
         *
         * 修复永远是 per-item 的动作，
         * 而"哪条 issue 属于哪个 item"在全库报告里已经丢失了。
         * 与其猜一个 item，不如显式拒绝。
         */
        val itemId =
            requireNotNull(report.itemId) {
                "A whole-database integrity report cannot be turned into a per-item repair plan. " +
                    "Run per-item checks instead."
            }

        return DynamicScheduleHistoryRepairPlan(
            itemId = itemId,

            integrityReport = report,

            /*
             * 没有问题也必须显式给出 NoOp，
             * 而不是空列表：
             *
             * "已经检查过，确认无需处理"
             * 和
             * "什么都没检查"
             * 对上层是两件事。
             */
            actions =
                actions.ifEmpty {
                    listOf(
                        DynamicScheduleHistoryRepairAction.NoOp,
                    )
                },
        )
    }

    private fun actionFor(
        issue: DynamicScheduleHistoryIntegrityIssue,
    ): DynamicScheduleHistoryRepairAction =
        when (issue) {

            /*
             * 13.17：孤儿不再由这里产出。
             *
             * 它由 plan() 里的并集统一产出，
             * 否则会因为"完整性报告 + 分析器"两个来源而重复。
             * 这里保留分支只是为了让 when 保持穷尽。
             */
            is DynamicScheduleHistoryIntegrityIssue
                .ReplayInputMissingSnapshot ->
                DynamicScheduleHistoryRepairAction
                    .NoOp

            is DynamicScheduleHistoryIntegrityIssue
                .MissingTrace ->
                DynamicScheduleHistoryRepairAction
                    .PreserveMissingTrace(
                        snapshotId = issue.snapshotId,
                        traceId = issue.traceId,
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .SnapshotWithoutTrace ->
                DynamicScheduleHistoryRepairAction
                    .PreserveSnapshotWithoutTrace(
                        snapshotId = issue.snapshotId,
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .MissingReplayInput ->
                DynamicScheduleHistoryRepairAction
                    .PreserveMissingReplayInput(
                        snapshotId = issue.snapshotId,
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .TraceItemMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Snapshot itemId '" +
                                issue.snapshotItemId +
                                "' != Trace itemId '" +
                                issue.traceItemId +
                                "'.",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .ScheduleRevisionMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Snapshot scheduleRevision " +
                                issue.snapshotRevision +
                                " != Trace scheduleRevision " +
                                issue.traceRevision +
                                ".",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .EngineVersionMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Snapshot engineVersion '" +
                                issue.snapshotEngineVersion +
                                "' != Trace engineVersion '" +
                                issue.traceEngineVersion +
                                "'.",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .ReplayInputSnapshotMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "ReplayInput references snapshot '" +
                                issue.replaySnapshotId.value +
                                "' instead of '" +
                                issue.snapshotId.value +
                                "'.",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .ReplayInputRevisionMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Snapshot scheduleRevision " +
                                issue.snapshotRevision +
                                " != ReplayInput scheduleRevision " +
                                issue.replayRevision +
                                ".",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .ReplayInputEngineVersionMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Snapshot engineVersion '" +
                                issue.snapshotEngineVersion +
                                "' != ReplayInput engineVersion '" +
                                issue.replayEngineVersion +
                                "'.",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .ReplayInputItemMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Snapshot itemId '" +
                                issue.itemId.value +
                                "' != ReplayInput itemId '" +
                                issue.replayItemId +
                                "'.",
                    )


            /*
             * Provenance 的引用缺失一律**保留**：
             * Snapshot 被压缩掉之后，Provenance 往往正是
             * 唯一还能说明"这里曾经发生过一次决策"的东西。
             */
            is DynamicScheduleHistoryIntegrityIssue
                .ProvenanceMissingSnapshot ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Provenance " +
                                issue.provenanceId.value +
                                " no longer has its decision snapshot.",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .ProvenanceMissingReplayInput ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = issue.snapshotId,

                        description =
                            "Provenance " +
                                issue.provenanceId.value +
                                " no longer has its replay input.",
                    )

            is DynamicScheduleHistoryIntegrityIssue
                .VerificationWithoutReplay ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        /*
                         * 该 issue 是按 provenance 定位的，
                         * 没有 snapshotId。
                         */
                        snapshotId = null,

                        description =
                            "Provenance " +
                                issue.provenanceId.value +
                                " claims verification without complete replay facts.",
                    )

            /*
             * 13.17.9：第一版不会产生这个 issue。
             * 保留分支只是为了模型完整。
             */
            is DynamicScheduleHistoryIntegrityIssue
                .VerificationEngineMismatch ->
                DynamicScheduleHistoryRepairAction
                    .PreserveConflict(
                        snapshotId = null,

                        description =
                            "Replay engine " +
                                issue.replayEngineVersion +
                                " differs from verification engine " +
                                issue.verificationEngineVersion +
                                ".",
                    )
        }
}
