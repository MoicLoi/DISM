package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 12.x / 13.13
 *
 * 统一历史决策查询结果。
 *
 * 约定：
 *
 * ```text
 * entries[0]
 * ```
 *
 * 就是**最新的一次**决策。
 */
data class DynamicScheduleDecisionHistoryResult(
    val entries: List<DynamicScheduleDecisionHistoryEntry>,

    val truncated: Boolean = false,

    val itemId: DynamicScheduleItemId? = null,

    /**
     * 满足条件的总条数（可能大于 [entries] 的长度）。
     */
    val totalMatched: Int = entries.size,
) {

    val isEmpty: Boolean
        get() = entries.isEmpty()

    val size: Int
        get() = entries.size

    val latest: DynamicScheduleDecisionHistoryEntry?
        get() = entries.firstOrNull()
}
