package me.moiclathy.dism.schedule.model

import me.moiclathy.dism.api.DismPublicApi
import kotlinx.serialization.Serializable

/**
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
@Serializable
@DismPublicApi
data class DynamicScheduleItemId(val value: String) {
    init {
        require(value.isNotBlank()) { "DynamicScheduleItemId must not be blank" }
    }

    override fun toString(): String = value
}
