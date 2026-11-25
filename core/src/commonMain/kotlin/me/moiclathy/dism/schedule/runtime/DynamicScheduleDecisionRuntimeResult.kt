package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

data class DynamicScheduleDecisionRuntimeResult(
    val intelligence:
        DynamicScheduleIntelligenceResult,

    val snapshots:
        List<DynamicScheduleDecisionSnapshot>,

    val traces:
        List<DynamicScheduleDecisionTraceRecord>,

    val diagnostics:
        List<String> = emptyList(),
)
