package me.moiclathy.dism.event.model

import kotlin.time.Instant

/**
 * Event 的时间语义。
 *
 * 这里只描述“已知的时间信息”。
 *
 * 不负责：
 *
 * - Duration 计算
 * - Slack
 * - Travel Time
 * - Alarm
 * - Replanning
 */
sealed interface EventTemporal {

    /**
     * 没有明确时间。
     *
     * 例如：
     *
     * “有空去买实验服。”
     */
    data object Unscheduled : EventTemporal

    /**
     * 某一个时间点发生。
     *
     * 例如：
     *
     * “明天下午三点实验课。”
     */
    data class At(
        val start: Instant,
    ) : EventTemporal

    /**
     * 有明确开始和结束时间。
     *
     * 例如：
     *
     * “14:00 - 16:00 实验课。”
     */
    data class Interval(
        val start: Instant,
        val end: Instant,
    ) : EventTemporal {

        init {
            require(end >= start) {
                "Event interval end must not be before start."
            }
        }
    }

    /**
     * 只有截止时间。
     *
     * 例如：
     *
     * “今晚之前交实验报告。”
     */
    data class Deadline(
        val deadline: Instant,
    ) : EventTemporal
}
