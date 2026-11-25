package me.moiclathy.dism.presentation.home

/**
 * 16.4：Today 页一次渲染所需的全部状态。
 *
 * ### 它不认识 Core 模型
 *
 * 这里**没有** `DynamicScheduleItem` / `DynamicScheduleDecisionView` /
 * `RuntimeSnapshot`。Compose 只认识这个类型。
 *
 * ```text
 * Core  →  Presenter  →  HomeUiState  →  Compose
 * ```
 *
 * 好处不只是"分层好看"：换了 Core 内部模型，UI 与 UI 测试都不用改。
 */
data class HomeUiState(
    /**
     * 形如 "Wed, Sep 9"。
     */
    val dateLabel: String,

    /**
     * 已按时间排序的日程条目。
     */
    val timeline: List<TimelineEntryUi> = emptyList(),

    /**
     * 经过 Attention 之后，真正需要用户现在知道的少数几条。
     */
    val attention: List<AttentionEntryUi> = emptyList(),

    val isLoading: Boolean = false,

    /**
     * 加载失败时的可读信息。
     *
     * 刻意不是 `Throwable`：UI 不应该被迫理解内部异常类型。
     */
    val errorMessage: String? = null,
) {

    val isEmpty: Boolean
        get() = timeline.isEmpty() && attention.isEmpty()

    /**
     * 有多少条需要用户注意。
     */
    val attentionCount: Int
        get() = attention.size

    fun entry(
        itemId: String,
    ): TimelineEntryUi? =
        timeline.firstOrNull { it.itemId == itemId }
}

/**
 * 时间线上的一条。
 *
 * `timeLabel` 是**算好的字符串**，不是 `Instant`：
 * 展示层不重新解释时间，格式化只发生在一处（[TimeFormatting]）。
 */
data class TimelineEntryUi(
    val itemId: String,

    val title: String,

    /**
     * "09:00"；没有具体时间时为 null（例如 Deadline / Unscheduled）。
     */
    val timeLabel: String?,

    /**
     * "09:00 – 09:40"；没有区间时为 null。
     */
    val rangeLabel: String?,

    /**
     * 次要说明行，例如地点。
     */
    val subtitle: String?,

    /**
     * 0.0..1.0，用于排序与视觉效果。
     */
    val priority: Double,

    /**
     * 是否被 Attention 判定为需要用户关注。
     */
    val needsAttention: Boolean,

    /**
     * 决策解释；没有时为 null。
     */
    val explanation: String?,
)

/**
 * Attention 区的一条。
 */
data class AttentionEntryUi(
    val itemId: String,

    val title: String,

    /**
     * 人类可读的原因。
     */
    val reason: String,
)
