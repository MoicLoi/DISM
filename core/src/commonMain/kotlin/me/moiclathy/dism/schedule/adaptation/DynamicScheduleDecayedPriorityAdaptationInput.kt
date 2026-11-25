package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

data class DynamicScheduleDecayedPriorityAdaptationInput(
    val model: DynamicScheduleModel,

    val histories:
        Map<String, DynamicScheduleDecayedAttentionHistory> =
        emptyMap(),
)
