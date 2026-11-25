package me.moiclathy.dism.schedule.runtime

data class DynamicScheduleDecisionRuntimeEvaluation(
    val intelligence:
        DynamicScheduleIntelligenceResult,

    val persistence:
        DynamicScheduleDecisionPersistenceResult,
) {

    val snapshots:
        List<me.moiclathy.dism.schedule.snapshot
            .DynamicScheduleDecisionSnapshot>
        get() =
            persistence.snapshots

    val traces:
        List<me.moiclathy.dism.schedule.trace
            .DynamicScheduleDecisionTraceRecord>
        get() =
            persistence.traces
}
