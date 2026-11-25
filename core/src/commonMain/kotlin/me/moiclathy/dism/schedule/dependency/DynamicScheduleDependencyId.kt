package me.moiclathy.dism.schedule.dependency

import kotlinx.serialization.Serializable

/**
 * 使用 data class 而非 value class，
 * 因为 @JvmInline 在 KMP common source set 中不可用。
 */
@Serializable
data class DynamicScheduleDependencyId(val value: String) {
    init {
        require(value.isNotBlank()) { "DynamicScheduleDependencyId must not be blank" }
    }

    override fun toString(): String = value
}
