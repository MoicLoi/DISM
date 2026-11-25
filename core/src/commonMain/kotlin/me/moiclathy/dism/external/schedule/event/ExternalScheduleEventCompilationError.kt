package me.moiclathy.dism.external.schedule.event

/**
 * Event 编译阶段的结构化错误。
 */
data class ExternalScheduleEventCompilationError(
    val code: Code,
    val message: String,
    val sourceEntryId: String? = null,
) {

    enum class Code {

        /**
         * 外部条目没有可用标题。
         */
        MISSING_TITLE,

        /**
         * 时间范围非法。
         */
        INVALID_TIME_RANGE,

        /**
         * 外部 ID 非法。
         */
        INVALID_SOURCE_ID,

        /**
         * 无法生成稳定 external key。
         */
        INVALID_EXTERNAL_KEY,

        /**
         * 当前策略禁止生成无时间 Event。
         */
        UNSCHEDULED_NOT_ALLOWED,

        /**
         * 其他编译错误。
         */
        INTERNAL_ERROR,
    }
}
