package me.moiclathy.dism.event.constraint

import kotlin.time.Instant

/**
 * 时间约束。
 *
 * 一个 Constraint 描述：
 *
 * “Event 的时间必须满足什么条件。”
 */
data class TimeConstraint(
    val type: TimeConstraintType,
    val start: Instant? = null,
    val end: Instant? = null,
    val reason: String? = null,
) {

    init {

        when (type) {

            TimeConstraintType.EARLIEST_START -> {
                require(start != null) {
                    "EARLIEST_START requires start."
                }
            }

            TimeConstraintType.LATEST_START -> {
                require(start != null) {
                    "LATEST_START requires start."
                }
            }

            TimeConstraintType.LATEST_END -> {
                require(end != null) {
                    "LATEST_END requires end."
                }
            }

            TimeConstraintType.FIXED_TIME -> {
                require(start != null) {
                    "FIXED_TIME requires start."
                }

                require(end == null) {
                    "FIXED_TIME must not have end."
                }
            }

            TimeConstraintType.TIME_WINDOW -> {
                require(start != null) {
                    "TIME_WINDOW requires start."
                }

                require(end != null) {
                    "TIME_WINDOW requires end."
                }

                require(start <= end) {
                    "TIME_WINDOW start must be <= end."
                }
            }
        }

        if (reason != null) {
            require(reason.isNotBlank()) {
                "reason must not be blank."
            }
        }
    }
}
