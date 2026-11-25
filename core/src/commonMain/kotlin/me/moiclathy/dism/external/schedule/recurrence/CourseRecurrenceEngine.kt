package me.moiclathy.dism.external.schedule.recurrence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * Course Recurrence 展开接口。
 */
interface CourseRecurrenceEngine {

    fun expand(
        sourceEntryId: ExternalScheduleId,
        startAt: Instant,
        rule: CourseRecurrenceRule,
        exceptions: List<CourseRecurrenceException> = emptyList(),
        expansionStart: Instant = startAt,
        expansionEnd: Instant? = null,
    ): CourseRecurrenceResult
}
