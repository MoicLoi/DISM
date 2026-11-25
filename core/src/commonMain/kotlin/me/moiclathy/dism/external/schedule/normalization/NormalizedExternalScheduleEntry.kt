package me.moiclathy.dism.external.schedule.normalization

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 规范化后的 External Schedule Entry。
 *
 * 注意：
 * original* 字段用于保留规范化前的信息。
 */
data class NormalizedExternalScheduleEntry(
    val id: ExternalScheduleId,
    val title: String,
    val startAt: Instant?,
    val endAt: Instant?,
    val location: String?,
    val instructor: String?,
    val courseCode: String?,
    val description: String?,
    val metadata: Map<String, String> = emptyMap(),
    val originalTitle: String? = null,
    val originalLocation: String? = null,
    val originalInstructor: String? = null,
    val originalCourseCode: String? = null,
    val originalDescription: String? = null,
) {

    init {
        require(title.isNotBlank()) {
            "Normalized entry title must not be blank."
        }

        if (location != null) {
            require(location.isNotBlank()) {
                "Normalized entry location must not be blank."
            }
        }

        if (instructor != null) {
            require(instructor.isNotBlank()) {
                "Normalized entry instructor must not be blank."
            }
        }

        if (courseCode != null) {
            require(courseCode.isNotBlank()) {
                "Normalized entry courseCode must not be blank."
            }
        }

        if (description != null) {
            require(description.isNotBlank()) {
                "Normalized entry description must not be blank."
            }
        }

        if (startAt != null && endAt != null) {
            require(startAt <= endAt) {
                "Normalized entry startAt must not be after endAt."
            }
        }
    }
}
