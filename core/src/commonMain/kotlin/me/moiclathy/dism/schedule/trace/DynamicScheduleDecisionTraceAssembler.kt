package me.moiclathy.dism.schedule.trace

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

interface DynamicScheduleDecisionTraceAssembler {

    fun assemble(
        input: DynamicScheduleDecisionTraceInput,
    ): DynamicScheduleDecisionTrace
}
