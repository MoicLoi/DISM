package me.moiclathy.dism.event.constraint

/**
 * 时间约束类型。
 */
enum class TimeConstraintType {

    /**
     * Event 不允许早于指定时间开始。
     *
     * earliestStart = 09:30
     */
    EARLIEST_START,

    /**
     * Event 不允许晚于指定时间开始。
     *
     * latestStart = 09:50
     */
    LATEST_START,

    /**
     * Event 不允许晚于指定时间结束。
     *
     * latestEnd = 11:30
     */
    LATEST_END,

    /**
     * Event 必须在指定时间点发生。
     *
     * 例如：
     *
     * “考试 10:00 开始”
     */
    FIXED_TIME,

    /**
     * Event 必须位于指定时间区间。
     *
     * 例如：
     *
     * “午饭安排在 11:30–13:30”
     */
    TIME_WINDOW,
}
