package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import kotlinx.serialization.Serializable

@Serializable
data class DynamicScheduleContextAttentionKey(
    val itemId: DynamicScheduleItemId,
    val contextKey: String,
) {
    init {
        require(contextKey.isNotBlank())
    }

    override fun toString(): String {
        return "${itemId.value}::$contextKey"
    }
}
