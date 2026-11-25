package me.moiclathy.dism.external.schedule.entity

/**
 * External Schedule 字段与 DISM Entity 的绑定类型。
 */
enum class ExternalScheduleEntityBindingType {

    /**
     * 外部课程 → COURSE
     */
    COURSE,

    /**
     * 外部教师 → PERSON
     */
    INSTRUCTOR,

    /**
     * 外部地点 → LOCATION
     */
    LOCATION,
}
