package me.moiclathy.dism.event.alarm

import kotlin.time.Instant

/**
 * Dynamic Alarm 计算结果。
 */
data class DynamicAlarmEvaluation(
    val alarm: DynamicAlarm?,
    val evaluatedAt: Instant,
    val generated: Boolean,
    val reason: String? = null,
) {

    init {

        if (generated) {
            require(alarm != null) {
                "Generated evaluation must contain an alarm."
            }
        }

        if (!generated) {
            require(alarm == null) {
                "Non-generated evaluation must not contain an alarm."
            }
        }

        if (reason != null) {
            require(reason.isNotBlank()) {
                "reason must not be blank."
            }
        }
    }
}
