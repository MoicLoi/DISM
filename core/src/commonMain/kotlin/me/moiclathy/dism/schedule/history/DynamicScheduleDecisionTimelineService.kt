package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 13.14.7
 *
 * 职责划分：
 *
 * ```text
 * HistoryService  → 数据库历史查询
 * TimelineService → 把查询结果解释成时间线
 * ```
 *
 * ### 永远不重放（13.14.10）
 *
 * [timeline] 只做：
 *
 * ```text
 * Store → Snapshot → Trace → Timeline
 * ```
 *
 * 而**不会**做：
 *
 * ```text
 * Store → Snapshot → Replay → Verification → Timeline
 * ```
 *
 * 否则用户打开"历史记录"就可能触发大量 Runtime 计算。
 *
 * > **Timeline 是历史视图；Replay Batch 是计算操作。**
 */
class DynamicScheduleDecisionTimelineService(
    private val history:
        DynamicScheduleDecisionHistoryService,
) {

    suspend fun timeline(
        itemId: DynamicScheduleItemId,

        limit: Int = 50,

        includeTrace: Boolean = true,
    ): DynamicScheduleDecisionTimeline {

        val result =
            history.query(
                DynamicScheduleDecisionHistoryQuery(
                    itemId = itemId,
                    limit = limit,
                    includeTrace = includeTrace,

                    /*
                     * 显式写出来，而不是依赖默认值：
                     * 时间线视图在任何情况下都不允许触发重放。
                     */
                    includeReplay = false,
                    includeVerification = false,
                ),
            )

        return DynamicScheduleDecisionTimeline.from(
            itemId = itemId,
            entries = result.entries,
        )
    }
}
