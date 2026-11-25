package me.moiclathy.dism.schedule.trace

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

data class DynamicScheduleDecisionTrace(
    val id: DynamicScheduleDecisionTraceId =
        DynamicScheduleDecisionTraceId(),

    val itemId: DynamicScheduleItemId,

    val entries:
        List<DynamicScheduleDecisionTraceEntry> =
        emptyList(),
) {

    val decisiveEntries:
        List<DynamicScheduleDecisionTraceEntry>
        get() = entries.filter { it.decisive }

    val stages:
        Set<DynamicScheduleDecisionTraceStage>
        get() = entries.map { it.stage }.toSet()

    fun append(
        entry: DynamicScheduleDecisionTraceEntry,
    ): DynamicScheduleDecisionTrace {
        return copy(
            entries = entries + entry,
        )
    }

    /**
     * 12.14 的小型 Trace DSL。
     *
     * 让 Runtime 可以直接累积决策阶段，
     * 而不需要到处手写 Entry 构造。
     */
    fun withEntry(
        stage: DynamicScheduleDecisionTraceStage,
        message: String,
        values: Map<String, String> = emptyMap(),
        decisive: Boolean = false,
    ): DynamicScheduleDecisionTrace {
        return append(
            DynamicScheduleDecisionTraceEntry(
                stage = stage,
                message = message,
                values = values,
                decisive = decisive,
            ),
        )
    }
}
