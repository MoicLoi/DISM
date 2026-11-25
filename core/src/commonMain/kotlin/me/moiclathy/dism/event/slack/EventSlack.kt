package me.moiclathy.dism.event.slack

import kotlin.time.Duration

/**
 * Event 的时间余量。
 *
 * Slack > 0:
 * 仍然存在时间余量。
 *
 * Slack = 0:
 * 已经抵达边界。
 *
 * Slack < 0:
 * 已经超出时间边界。
 */
data class EventSlack(
    val duration: Duration,
) {

    val isPositive: Boolean
        get() = duration > Duration.ZERO

    val isZero: Boolean
        get() = duration == Duration.ZERO

    val isNegative: Boolean
        get() = duration < Duration.ZERO
}
