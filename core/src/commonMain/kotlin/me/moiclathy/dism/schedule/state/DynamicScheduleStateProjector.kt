package me.moiclathy.dism.schedule.state

import me.moiclathy.dism.schedule.model.DynamicScheduleModel

/** Pure projection boundary. It must not persist or trigger Attention. */
fun interface DynamicScheduleStateProjector {
    fun project(input: DynamicScheduleStateProjectionInput): DynamicScheduleStateProjectionResult
}
