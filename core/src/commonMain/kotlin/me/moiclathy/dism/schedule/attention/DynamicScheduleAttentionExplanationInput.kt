package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.schedule.model.DynamicScheduleItem

data class DynamicScheduleAttentionExplanationInput(
    val item: DynamicScheduleItem,

    val conflictResult:
        DynamicScheduleAttentionConflictResult,

    val contextReasons:
        List<String> = emptyList(),

    val adaptationReasons:
        List<String> = emptyList(),
)
