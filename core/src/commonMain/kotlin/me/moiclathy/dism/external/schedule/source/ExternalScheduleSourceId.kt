package me.moiclathy.dism.external.schedule.source

/**
 * 外部日程来源实例的稳定 ID。
 *
 * 注意：
 * ExternalScheduleSourceId 与
 * ExternalScheduleId 不是同一个概念。
 *
 * ExternalScheduleId:
 *     一份具体的外部日程
 *
 * ExternalScheduleSourceId:
 *     一个“数据来源实例”
 *
 * 例如：
 *
 *     xiqueer-main
 *     school-ics
 *     local-import
 *
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
data class ExternalScheduleSourceId(
    val value: String,
) {

    init {
        require(value.isNotBlank()) {
            "ExternalScheduleSourceId must not be blank."
        }
    }

    override fun toString(): String =
        value
}
