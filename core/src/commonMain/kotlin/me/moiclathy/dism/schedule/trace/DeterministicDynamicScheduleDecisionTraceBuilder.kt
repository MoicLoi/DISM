package me.moiclathy.dism.schedule.trace

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DeterministicDynamicScheduleDecisionTraceBuilder :
    DynamicScheduleDecisionTraceBuilder {

    override fun create(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionTrace {
        return DynamicScheduleDecisionTrace(
            itemId = itemId,
        )
    }

    override fun append(
        trace: DynamicScheduleDecisionTrace,
        entry: DynamicScheduleDecisionTraceEntry,
    ): DynamicScheduleDecisionTrace {
        return trace.append(entry)
    }
}
