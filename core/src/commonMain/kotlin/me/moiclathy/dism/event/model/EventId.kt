package me.moiclathy.dism.event.model

/**
 * Event 的稳定身份。
 *
 * EventId 与 Event 的生命周期绑定。
 *
 * 不应该因为 Event 的：
 *
 * - 时间变化
 * - 标题变化
 * - State 变化
 * - 来源更新
 *
 * 而改变。
 *
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
data class EventId(
    val value: String,
) {

    init {
        require(value.isNotBlank()) {
            "EventId must not be blank."
        }
    }

    override fun toString(): String = value
}
