package me.moiclathy.dism.external.schedule

import kotlin.time.Instant

/**
 * 一个外部日程条目。
 *
 * 它描述的是“外部系统提供了什么”，
 * 而不是 DISM 最终认为它“意味着什么”。
 */
data class ExternalScheduleEntry(
    val id: ExternalScheduleId,

    /**
     * 外部系统显示的标题。
     */
    val title: String,

    /**
     * 外部来源。
     */
    val source: ExternalScheduleOrigin,

    /**
     * 开始时间。
     */
    val startAt: Instant? = null,

    /**
     * 结束时间。
     */
    val endAt: Instant? = null,

    /**
     * 地点原始文本。
     */
    val location: String? = null,

    /**
     * 教师 / 参与者等原始文本。
     */
    val instructor: String? = null,

    /**
     * 课程编号、科目代码等原始标识。
     */
    val courseCode: String? = null,

    /**
     * 原始描述。
     */
    val description: String? = null,

    /**
     * 外部系统附加信息。
     */
    val metadata: ExternalScheduleMetadata =
        ExternalScheduleMetadata.EMPTY,
) {

    init {

        require(title.isNotBlank()) {
            "ExternalScheduleEntry title must not be blank."
        }

        if (startAt != null && endAt != null) {
            require(startAt <= endAt) {
                "ExternalScheduleEntry startAt must not be after endAt."
            }
        }

        validateOptionalText(
            location,
            "location",
        )

        validateOptionalText(
            instructor,
            "instructor",
        )

        validateOptionalText(
            courseCode,
            "courseCode",
        )
    }

    /**
     * 是否具有完整时间区间。
     */
    val hasInterval: Boolean
        get() =
            startAt != null &&
                endAt != null

    /**
     * 是否至少具有开始时间。
     */
    val hasStartTime: Boolean
        get() =
            startAt != null

    /**
     * 是否具有地点。
     */
    val hasLocation: Boolean
        get() =
            !location.isNullOrBlank()

    /**
     * 是否具有教师信息。
     */
    val hasInstructor: Boolean
        get() =
            !instructor.isNullOrBlank()

    private fun validateOptionalText(
        value: String?,
        fieldName: String,
    ) {
        if (value != null) {
            require(value.isNotBlank()) {
                "$fieldName must not be blank when supplied."
            }
        }
    }
}
