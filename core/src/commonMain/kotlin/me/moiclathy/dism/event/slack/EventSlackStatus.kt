package me.moiclathy.dism.event.slack

/**
 * Slack 的离散状态。
 */
enum class EventSlackStatus {

    /**
     * 仍有明显时间余量。
     */
    AVAILABLE,

    /**
     * 已经非常接近约束边界。
     */
    CRITICAL,

    /**
     * 正好抵达约束边界。
     */
    ZERO,

    /**
     * 已经超过约束边界。
     */
    OVERDUE,

    /**
     * 无法计算 Slack。
     */
    UNKNOWN,
}
