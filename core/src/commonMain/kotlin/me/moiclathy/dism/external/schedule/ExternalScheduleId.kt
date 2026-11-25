package me.moiclathy.dism.external.schedule

/**
 * 外部日程源中的稳定标识。
 *
 * 注意：
 * 这里不是 DISM EventId。
 *
 * ExternalScheduleId 表示“外部系统认为这个对象是谁”，
 * 后续 11.8 才负责把它映射到 DISM 内部 Event。
 *
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
data class ExternalScheduleId(
    val value: String,
) {

    init {
        require(value.isNotBlank()) {
            "ExternalScheduleId must not be blank."
        }
    }

    override fun toString(): String =
        value
}
