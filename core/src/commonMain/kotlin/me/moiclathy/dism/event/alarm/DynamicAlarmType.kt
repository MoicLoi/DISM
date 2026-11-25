package me.moiclathy.dism.event.alarm

/**
 * Dynamic Alarm 的类型。
 */
enum class DynamicAlarmType {

    /**
     * 普通时间提醒。
     *
     * 在事件开始前固定提前量提醒。
     */
    BEFORE_START,

    /**
     * 根据事件结束约束计算提醒。
     *
     * 例如：
     *
     * latestEnd = 11:30
     * preparation = 20m
     *
     * 系统可能在 11:10 提醒。
     */
    BEFORE_DEADLINE,

    /**
     * 当 Slack 进入临界区间时提醒。
     */
    SLACK_CRITICAL,

    /**
     * 当 Slack 已经耗尽时触发。
     */
    SLACK_EXHAUSTED,
}
