package me.moiclathy.dism.schedule.history

/**
 * 13.17
 *
 * 由 issue 列表推断整体状态。
 *
 * 抽成顶层函数是因为它有两个使用者：
 *
 * ```text
 * 单 item 检查（13.7 的 Checker）
 * 全库检查（13.17）
 * ```
 *
 * 两处规则必须**完全一致**，否则会出现
 * "单 item 说 BROKEN、全库说 DEGRADED"这种自相矛盾。
 *
 * 规则：
 *
 * ```text
 * 没有任何 issue        → VALID
 * 只有"缺失"            → DEGRADED
 * 存在任何"矛盾"        → BROKEN
 * ```
 *
 * BROKEN 优先：数据互相矛盾时，
 * 即使同时还有缺失，也必须报最严重的状态。
 */
internal fun determineIntegrityStatus(
    issues: List<DynamicScheduleHistoryIntegrityIssue>,
): DynamicScheduleHistoryIntegrityStatus {

    if (issues.isEmpty()) {
        return DynamicScheduleHistoryIntegrityStatus.VALID
    }

    val hasStructuralConflict =
        issues.any { issue ->
            when (issue) {

                is DynamicScheduleHistoryIntegrityIssue
                    .TraceItemMismatch,
                is DynamicScheduleHistoryIntegrityIssue
                    .ScheduleRevisionMismatch,
                is DynamicScheduleHistoryIntegrityIssue
                    .EngineVersionMismatch,
                is DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputSnapshotMismatch,
                is DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputRevisionMismatch,
                is DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputEngineVersionMismatch,
                is DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputItemMismatch,
                is DynamicScheduleHistoryIntegrityIssue
                    .VerificationEngineMismatch ->
                true

                is DynamicScheduleHistoryIntegrityIssue
                    .MissingTrace,
                is DynamicScheduleHistoryIntegrityIssue
                    .MissingReplayInput,
                is DynamicScheduleHistoryIntegrityIssue
                    .SnapshotWithoutTrace,

                /*
                 * 13.17：反方向引用缺失属于"缺"而不是"矛盾"。
                 *
                 * 尤其 ProvenanceMissingSnapshot 是 Retention
                 * 的正常结果（13.12.10 明确不让 Provenance 级联删除），
                 * 把它判成 BROKEN 会让每次压缩历史都"损坏"一次数据库。
                 */
                is DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputMissingSnapshot,
                is DynamicScheduleHistoryIntegrityIssue
                    .ProvenanceMissingSnapshot,
                is DynamicScheduleHistoryIntegrityIssue
                    .ProvenanceMissingReplayInput,
                is DynamicScheduleHistoryIntegrityIssue
                    .VerificationWithoutReplay ->
                false
            }
        }

    return if (hasStructuralConflict) {
        DynamicScheduleHistoryIntegrityStatus.BROKEN
    } else {
        DynamicScheduleHistoryIntegrityStatus.DEGRADED
    }
}
