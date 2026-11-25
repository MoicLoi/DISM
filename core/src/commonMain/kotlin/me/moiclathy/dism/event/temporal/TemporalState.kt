package me.moiclathy.dism.event.temporal

/**
 * Event 当前所处的时间状态。
 *
 * 该状态由：
 *
 * EventTemporal + evaluatedAt
 *
 * 推导得到。
 *
 * 它不是 EventState。
 */
enum class TemporalState {

    /**
     * 没有时间安排。
     *
     * 对应 EventTemporal.Unscheduled。
     */
    UNSCHEDULED,

    /**
     * 事件尚未开始，但已经有明确时间。
     */
    UPCOMING,

    /**
     * 事件当前正在进行。
     */
    IN_PROGRESS,

    /**
     * 事件已经结束。
     */
    ENDED,

    /**
     * Deadline 已经到达，但尚未超过。
     */
    DUE,

    /**
     * Deadline 已经超过。
     */
    OVERDUE,
}
