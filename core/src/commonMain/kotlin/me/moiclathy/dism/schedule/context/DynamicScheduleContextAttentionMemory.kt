package me.moiclathy.dism.schedule.context

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

interface DynamicScheduleContextAttentionMemory {

    fun evaluate(
        input: DynamicScheduleContextAttentionInput,
        policy: DynamicScheduleContextAttentionPolicy =
            DynamicScheduleContextAttentionPolicy(),
    ): DynamicScheduleContextAttentionResult

    fun record(
        history:
            DynamicScheduleContextAttentionHistory,
    )
}
