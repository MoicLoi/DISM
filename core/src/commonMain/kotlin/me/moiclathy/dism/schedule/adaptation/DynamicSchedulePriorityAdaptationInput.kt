package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

data class DynamicSchedulePriorityAdaptationInput(
    val model: DynamicScheduleModel,

    /**
     * 历史 Attention 行为。
     *
     * key = AttentionKey / stable recommendation identity
     */
    val histories: Map<String, DynamicScheduleAttentionHistory> =
        emptyMap(),
)
