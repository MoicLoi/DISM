package me.moiclathy.dism.external.schedule.recurrence

import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant

/**
 * 一个已经结构化的课程重复规则。
 *
 * 对应 ICS 中的：
 *
 * RRULE:FREQ=WEEKLY;INTERVAL=1;BYDAY=MO,WE;COUNT=16
 *
 * 注意：
 * 该模型只表达规则，
 * 不负责实例展开。
 */
data class CourseRecurrenceRule(
    val frequency: RecurrenceFrequency,
    val interval: Int = 1,
    val count: Int? = null,
    val until: Instant? = null,
    val byDay: List<DayOfWeek> = emptyList(),
) {

    init {
        require(interval > 0) {
            "Recurrence interval must be greater than zero."
        }

        require(
            count == null || count > 0
        ) {
            "Recurrence count must be greater than zero."
        }

        require(
            count == null || until == null
        ) {
            "Recurrence rule cannot define both COUNT and UNTIL."
        }

        require(
            byDay.distinct().size == byDay.size
        ) {
            "Recurrence BYDAY must not contain duplicates."
        }
    }

    val isFinite: Boolean
        get() =
            count != null || until != null

    val hasDayFilter: Boolean
        get() =
            byDay.isNotEmpty()
}
