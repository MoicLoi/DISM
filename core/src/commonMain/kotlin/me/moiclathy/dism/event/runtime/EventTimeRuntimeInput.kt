package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

/**
 * Time-flow Runtime 单次评估输入。
 */
data class EventTimeRuntimeInput(
    val evaluatedAt: Instant,
    val eventCount: Int,
) {

    init {
        require(eventCount >= 0) {
            "eventCount must not be negative."
        }
    }
}
