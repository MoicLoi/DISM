package me.moiclathy.dism.external.schedule.event

/**
 * External Schedule → DISM Event Factory 错误。
 */
data class ExternalScheduleEventFactoryError(
    val code: Code,
    val message: String,
    val externalKey: String? = null,
) {

    enum class Code {

        /**
         * Draft 无法转换。
         */
        INVALID_DRAFT,

        /**
         * Event Factory 没有返回 Event。
         */
        FACTORY_REJECTED,

        /**
         * 外部身份无效。
         */
        INVALID_EXTERNAL_KEY,

        /**
         * 当前策略禁止该类型 Event。
         */
        POLICY_REJECTED,

        /**
         * Factory 内部错误。
         */
        INTERNAL_ERROR,
    }
}
