package me.moiclathy.dism.schedule.context

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleContextAttentionInput(
    val itemId: DynamicScheduleItemId,

    val activeContextKeys: Set<String>,

    val histories:
        Map<DynamicScheduleContextAttentionKey,
            DynamicScheduleContextAttentionHistory>,

    val now: Instant,
)
