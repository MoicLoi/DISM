package me.moiclathy.dism.external.schedule.parser

/**
 * Parser 错误类型。
 */
data class ExternalScheduleParseError(
    val code: ExternalScheduleParseErrorCode,
    val message: String,
    val field: String? = null,
) {

    init {
        require(message.isNotBlank()) {
            "Parser error message must not be blank."
        }

        if (field != null) {
            require(field.isNotBlank()) {
                "Parser error field must not be blank."
            }
        }
    }
}

/**
 * Parser 错误代码。
 */
enum class ExternalScheduleParseErrorCode {

    /**
     * 原始数据为空或无效。
     */
    INVALID_INPUT,

    /**
     * 格式无法识别。
     */
    UNSUPPORTED_FORMAT,

    /**
     * 数据结构损坏。
     */
    MALFORMED_DATA,

    /**
     * 缺少必要字段。
     */
    MISSING_FIELD,

    /**
     * 时间数据无法解析。
     */
    INVALID_TIME,

    /**
     * 条目数据无效。
     */
    INVALID_ENTRY,

    /**
     * 存在重复 ID。
     */
    DUPLICATE_ID,

    /**
     * Parser 内部错误。
     */
    INTERNAL_ERROR,
}
