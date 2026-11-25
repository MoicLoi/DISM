package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant

data class DynamicScheduleAttentionDecayInput(
    val histories:
        Map<String, DynamicScheduleAttentionHistory> = emptyMap(),

    val now: Instant,
)
