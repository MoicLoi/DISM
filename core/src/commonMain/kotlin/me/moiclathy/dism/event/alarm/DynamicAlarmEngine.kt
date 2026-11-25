package me.moiclathy.dism.event.alarm

import kotlin.time.Instant
import kotlin.time.Duration

/**
 * Dynamic Alarm Engine。
 */
interface DynamicAlarmEngine {

    /**
     * 根据事件开始时间生成动态提醒。
     */
    fun beforeStart(
        startAt: Instant?,
        evaluatedAt: Instant,
        leadTime: Duration? = null,
    ): DynamicAlarmEvaluation

    /**
     * 根据 deadline 生成动态提醒。
     */
    fun beforeDeadline(
        deadlineAt: Instant?,
        evaluatedAt: Instant,
        leadTime: Duration? = null,
    ): DynamicAlarmEvaluation

    /**
     * 根据 Slack 生成临界提醒。
     */
    fun evaluateSlack(
        boundary: Instant?,
        actual: Instant?,
        evaluatedAt: Instant,
    ): DynamicAlarmEvaluation
}
