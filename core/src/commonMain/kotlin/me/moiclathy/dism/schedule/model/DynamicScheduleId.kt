package me.moiclathy.dism.schedule.model

import kotlinx.serialization.Serializable

/**
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
@Serializable
data class DynamicScheduleId(val value: String) {
    init {
        require(value.isNotBlank()) { "DynamicScheduleId must not be blank" }
    }

    override fun toString(): String = value
}
