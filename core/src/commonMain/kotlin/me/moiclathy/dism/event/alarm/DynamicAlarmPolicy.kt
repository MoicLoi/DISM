package me.moiclathy.dism.event.alarm

import kotlin.time.Duration

/**
 * Dynamic Alarm 策略。
 */
data class DynamicAlarmPolicy(

    /**
     * 默认的事件开始提前提醒时间。
     */
    val defaultLeadTime: Duration =
        Duration.parse("15m"),

    /**
     * 当 Slack 小于等于这个值时，
     * 认为进入临界提醒区域。
     */
    val criticalSlackThreshold: Duration =
        Duration.parse("10m"),

    /**
     * 是否允许产生临界 Slack 提醒。
     */
    val allowCriticalSlackAlarm: Boolean = true,

    /**
     * 是否允许产生 Slack 耗尽提醒。
     */
    val allowSlackExhaustedAlarm: Boolean = true,
) {

    init {

        require(!defaultLeadTime.isNegative()) {
            "defaultLeadTime must not be negative."
        }

        require(!criticalSlackThreshold.isNegative()) {
            "criticalSlackThreshold must not be negative."
        }
    }
}
