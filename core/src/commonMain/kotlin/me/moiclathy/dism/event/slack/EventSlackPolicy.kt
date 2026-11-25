package me.moiclathy.dism.event.slack

import kotlin.time.Duration

/**
 * Slack 评估策略。
 */
data class EventSlackPolicy(

    /**
     * 小于等于该值时进入 CRITICAL。
     *
     * 默认 10 分钟。
     */
    val criticalThreshold: Duration =
        Duration.parse("10m"),
) {

    init {
        require(!criticalThreshold.isNegative()) {
            "criticalThreshold must not be negative."
        }
    }
}
