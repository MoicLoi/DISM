package me.moiclathy.dism.event.alarm

import kotlin.time.Instant
import kotlin.time.Duration

/**
 * 动态提醒计划。
 *
 * 它描述：
 *
 * “系统决定在什么时候，因为为什么提醒用户。”
 */
data class DynamicAlarm(
    val type: DynamicAlarmType,
    val triggerAt: Instant,
    val reason: String,
    val leadTime: Duration = Duration.ZERO,
) {

    init {
        require(reason.isNotBlank()) {
            "reason must not be blank."
        }

        require(!leadTime.isNegative()) {
            "leadTime must not be negative."
        }
    }
}
