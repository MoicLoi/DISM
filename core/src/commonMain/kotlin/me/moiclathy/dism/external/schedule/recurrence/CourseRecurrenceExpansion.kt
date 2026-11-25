package me.moiclathy.dism.external.schedule.recurrence

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一个 External Schedule Entry 展开后的实例。
 */
data class CourseRecurrenceInstance(
    val instanceId: String,
    val sourceEntryId: ExternalScheduleId,
    val occurrenceAt: Instant,
    val occurrenceIndex: Int,
) {

    init {
        require(instanceId.isNotBlank()) {
            "Recurrence instanceId must not be blank."
        }

        require(occurrenceIndex >= 0) {
            "Recurrence occurrenceIndex must not be negative."
        }
    }
}

/**
 * 一个课程重复展开结果。
 */
data class CourseRecurrenceExpansion(
    val sourceEntryId: ExternalScheduleId,
    val instances: List<CourseRecurrenceInstance>,
) {

    val size: Int
        get() =
            instances.size
}
