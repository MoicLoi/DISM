package me.moiclathy.dism.event.duration

import kotlin.time.Duration

/**
 * Event Duration 估计值。
 *
 * 用于描述可能存在不确定性的持续时间。
 */
data class EventDurationEstimate(
    val minimum: Duration,
    val expected: Duration,
    val maximum: Duration,
) {

    init {
        require(!minimum.isNegative()) {
            "minimum must not be negative."
        }

        require(!expected.isNegative()) {
            "expected must not be negative."
        }

        require(!maximum.isNegative()) {
            "maximum must not be negative."
        }

        require(minimum <= expected) {
            "minimum must be <= expected."
        }

        require(expected <= maximum) {
            "expected must be <= maximum."
        }
    }

    /**
     * 是否为精确 Duration。
     */
    val isExact: Boolean
        get() =
            minimum == expected &&
                expected == maximum
}
