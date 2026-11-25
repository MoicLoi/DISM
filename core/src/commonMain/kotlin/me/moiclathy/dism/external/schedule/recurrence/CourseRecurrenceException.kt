package me.moiclathy.dism.external.schedule.recurrence

import kotlin.time.Instant

/**
 * 单个 recurrence 实例的例外。
 */
sealed interface CourseRecurrenceException {

    val occurrenceAt: Instant

    /**
     * 删除一次正常重复产生的实例。
     *
     * 对应：
     *
     * EXDATE
     */
    data class Excluded(
        override val occurrenceAt: Instant,
    ) : CourseRecurrenceException

    /**
     * 增加一个正常 RRULE 之外的实例。
     *
     * 对应：
     *
     * RDATE
     */
    data class Added(
        override val occurrenceAt: Instant,
    ) : CourseRecurrenceException
}
