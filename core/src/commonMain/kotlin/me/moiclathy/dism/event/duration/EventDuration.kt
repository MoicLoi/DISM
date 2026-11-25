package me.moiclathy.dism.event.duration

import kotlin.time.Duration

/**
 * Event 的持续时间。
 */
data class EventDuration(
    val value: Duration,
    val type: EventDurationType = EventDurationType.FIXED,
) {

    init {
        require(!value.isNegative()) {
            "EventDuration must not be negative."
        }
    }

    companion object {

        fun zero(
            type: EventDurationType =
                EventDurationType.FIXED,
        ): EventDuration =
            EventDuration(
                value =
                    Duration.ZERO,

                type =
                    type,
            )
    }
}
