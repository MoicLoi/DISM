package me.moiclathy.dism.schedule.trace

interface DynamicScheduleDecisionTraceBuilder {

    fun create(
        itemId: me.moiclathy.dism.schedule.model.DynamicScheduleItemId,
    ): DynamicScheduleDecisionTrace

    fun append(
        trace: DynamicScheduleDecisionTrace,
        entry: DynamicScheduleDecisionTraceEntry,
    ): DynamicScheduleDecisionTrace
}
