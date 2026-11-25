package me.moiclathy.dism.external.schedule.recurrence

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days

/**
 * Course Recurrence 展开策略。
 */
data class CourseRecurrencePolicy(
    val maxOccurrences: Int = 128,
    val defaultExpansionWindow: Duration = 180.days,
    val allowDaily: Boolean = true,
    val allowWeekly: Boolean = true,
    val allowMonthly: Boolean = true,
    val allowYearly: Boolean = true,
) {

    init {
        require(maxOccurrences > 0) {
            "maxOccurrences must be greater than zero."
        }

        require(!defaultExpansionWindow.isNegative()) {
            "defaultExpansionWindow must not be negative."
        }
    }
}
