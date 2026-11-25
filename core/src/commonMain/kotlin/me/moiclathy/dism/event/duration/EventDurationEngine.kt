package me.moiclathy.dism.event.duration

/**
 * Event Duration Engine。
 *
 * 负责验证和规范化 Duration。
 *
 * 不负责：
 *
 * - 时间点计算
 * - Schedule
 * - Slack
 * - Alarm
 * - Replanning
 */
interface EventDurationEngine {

    fun resolve(
        duration: EventDuration,
    ): EventDurationResult

    fun resolveEstimate(
        estimate: EventDurationEstimate,
        type: EventDurationType =
            EventDurationType.ESTIMATED,
    ): EventDurationResult
}
