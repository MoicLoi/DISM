package me.moiclathy.dism.schedule.context

import kotlinx.serialization.Serializable

/**
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
@Serializable
data class DynamicScheduleContextBindingId(
    val value: String,
) {
    init {
        require(value.isNotBlank()) {
            "DynamicScheduleContextBindingId must not be blank"
        }
    }

    override fun toString(): String = value
}
