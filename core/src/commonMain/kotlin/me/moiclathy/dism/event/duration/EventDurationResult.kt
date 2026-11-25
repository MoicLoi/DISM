package me.moiclathy.dism.event.duration

/**
 * Duration Engine 的计算结果。
 */
data class EventDurationResult(
    val duration: EventDuration,
    val estimate: EventDurationEstimate? = null,
)
