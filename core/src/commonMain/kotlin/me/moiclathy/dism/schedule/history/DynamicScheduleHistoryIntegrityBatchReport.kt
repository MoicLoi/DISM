package me.moiclathy.dism.schedule.history

/**
 * 13.7
 *
 * 多个 item 的完整性报告。
 *
 * App 层真正需要的是这个：
 *
 * ```text
 * 启动时自检 → "我的历史还健康吗？"
 * ```
 *
 * 而不是逐个 item 自己拼。
 */
data class DynamicScheduleHistoryIntegrityBatchReport(
    val reports:
        List<DynamicScheduleHistoryIntegrityReport>,
) {

    val checkedItems: Int
        get() = reports.size

    val validItems: Int
        get() =
            reports.count {
                it.status ==
                    DynamicScheduleHistoryIntegrityStatus.VALID
            }

    val degradedItems: Int
        get() =
            reports.count {
                it.status ==
                    DynamicScheduleHistoryIntegrityStatus.DEGRADED
            }

    val brokenItems: Int
        get() =
            reports.count {
                it.status ==
                    DynamicScheduleHistoryIntegrityStatus.BROKEN
            }

    val checkedSnapshots: Int
        get() =
            reports.sumOf {
                it.checkedSnapshots
            }

    val totalIssues: Int
        get() =
            reports.sumOf {
                it.issueCount
            }

    val isHealthy: Boolean
        get() =
            reports.none {
                it.status !=
                    DynamicScheduleHistoryIntegrityStatus.VALID
            }
}
