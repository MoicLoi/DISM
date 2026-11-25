package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

interface DynamicScheduleDecisionReplayReportBuilder {

    fun build(
        replay:
            DynamicScheduleSnapshotReplayResult,

        originalTrace:
            DynamicScheduleDecisionTraceRecord? = null,
    ): DynamicScheduleDecisionReplayReport
}
