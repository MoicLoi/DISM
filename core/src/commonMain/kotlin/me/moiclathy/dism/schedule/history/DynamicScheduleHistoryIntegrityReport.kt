package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 13.7 / 13.17
 *
 * 历史完整性报告。
 *
 * 同一份类型同时服务两种检查：
 *
 * ```text
 * 单 item 检查  → itemId != null
 * 全库检查      → itemId == null（13.17 的双向引用完整性）
 * ```
 */
data class DynamicScheduleHistoryIntegrityReport(
    /**
     * 被检查的 item。
     *
     * 为 null 表示这是一次**全库**检查。
     */
    val itemId: DynamicScheduleItemId?,

    val status: DynamicScheduleHistoryIntegrityStatus,

    val checkedSnapshots: Int,

    val issues: List<DynamicScheduleHistoryIntegrityIssue> =
        emptyList(),

    /**
     * 13.17：全库检查时统计到的 ReplayInput 条数。
     */
    val checkedReplayInputs: Int = 0,

    /**
     * 13.17：全库检查时统计到的 Provenance 条数。
     */
    val checkedProvenances: Int = 0,

    /**
     * 13.17：检查执行时间。
     *
     * 由调用方传入，不在这里读系统时钟——
     * 同样的数据必须得到同样的报告。
     */
    val checkedAt: kotlin.time.Instant? = null,
) {

    init {
        require(checkedSnapshots >= 0)
        require(checkedReplayInputs >= 0)
        require(checkedProvenances >= 0)
    }

    val isValid: Boolean
        get() =
            status ==
                DynamicScheduleHistoryIntegrityStatus.VALID

    val isDegraded: Boolean
        get() =
            status ==
                DynamicScheduleHistoryIntegrityStatus.DEGRADED

    val isBroken: Boolean
        get() =
            status ==
                DynamicScheduleHistoryIntegrityStatus.BROKEN

    val issueCount: Int
        get() = issues.size

    /**
     * 13.17：没有任何问题。
     *
     * UI / CLI 可以直接：
     *
     * ```kotlin
     * if (report.isHealthy) { ... }
     * ```
     */
    val isHealthy: Boolean
        get() = issues.isEmpty()
}
