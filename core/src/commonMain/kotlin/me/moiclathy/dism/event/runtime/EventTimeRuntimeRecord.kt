package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

/**
 * 一次 Time-flow Runtime 评估记录。
 */
data class EventTimeRuntimeRecord(
    val evaluatedAt: Instant,
    val processedEventCount: Int,
    val generatedAlarmCount: Int,
) {

    init {
        require(processedEventCount >= 0) {
            "processedEventCount must not be negative."
        }

        require(generatedAlarmCount >= 0) {
            "generatedAlarmCount must not be negative."
        }

        require(generatedAlarmCount <= processedEventCount) {
            "generatedAlarmCount must not exceed processedEventCount."
        }
    }
}
