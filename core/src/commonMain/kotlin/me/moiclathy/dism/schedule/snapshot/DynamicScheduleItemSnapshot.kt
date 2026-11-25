package me.moiclathy.dism.schedule.snapshot

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState

data class DynamicScheduleItemSnapshot(
    val itemId: DynamicScheduleItemId,

    val title: String,

    val state: DynamicScheduleState,

    val priority: Double,

    val confidence: Double,
) {
    init {
        require(title.isNotBlank())
        require(priority in 0.0..1.0)
        require(confidence in 0.0..1.0)
    }
}
