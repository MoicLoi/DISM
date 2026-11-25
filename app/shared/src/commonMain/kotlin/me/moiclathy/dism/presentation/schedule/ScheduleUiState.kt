package me.moiclathy.dism.presentation.schedule

/**
 * 16.5：Schedule 页一次渲染所需的状态。
 *
 * 与 Today 的区别不只是"多几条"：
 *
 * ```text
 * Today     今天 + 经过 Attention 的少量条目
 * Schedule  全部（含已排期与未定时），按时间分组
 * ```
 *
 * 用户来 Schedule 页是**找**，所以这里要的是完整与可扫描，
 * 而不是"值得注意"。
 */
data class ScheduleUiState(
    /**
     * 按时间正序的全部条目。
     *
     * 与 Today 不同，这里**不过滤终态**：用户会想确认
     * "我确实做过那件事"。终态条目由 [ScheduleEntryUi.isTerminal]
     * 标记，UI 决定淡化还是折叠。
     */
    val entries: List<ScheduleEntryUi> = emptyList(),

    val isLoading: Boolean = false,

    val errorMessage: String? = null,
) {

    val isEmpty: Boolean
        get() = entries.isEmpty()

    val size: Int
        get() = entries.size

    /**
     * 还没结束的条目。
     */
    val activeEntries: List<ScheduleEntryUi>
        get() = entries.filterNot { it.isTerminal }

    /**
     * 没有具体时间的条目。
     *
     * 单独暴露是因为它们必须在 UI 上与时间轴分开显示，
     * 否则会变成时间轴末尾一堆"没有时间"的条目。
     */
    val unscheduledEntries: List<ScheduleEntryUi>
        get() = entries.filter { it.timeLabel == null }
}

/**
 * Schedule 页的一条。
 *
 * 比 `TimelineEntryUi` 多两个字段：
 *
 * ```text
 * stateLabel   用户需要知道"它现在是什么状态"
 * isTerminal   已完成/已取消/已过期
 * ```
 *
 * Today 页不需要它们，因为 Today 已经把终态过滤掉了。
 */
data class ScheduleEntryUi(
    val itemId: String,

    val title: String,

    val timeLabel: String?,

    val rangeLabel: String?,

    val subtitle: String?,

    /**
     * 状态的**可读**表示，例如 "Completed"。
     *
     * 刻意不是 `DynamicScheduleState`：UI 不该被迫认识 Core 枚举，
     * 也不该在枚举新增值时全部崩掉。
     */
    val stateLabel: String,

    val isTerminal: Boolean,

    val priority: Double,

    val needsAttention: Boolean,
)
