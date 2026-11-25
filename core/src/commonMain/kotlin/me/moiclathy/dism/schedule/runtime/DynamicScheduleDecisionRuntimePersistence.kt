package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

interface DynamicScheduleDecisionRuntimePersistence {

    suspend fun persistSnapshot(
        snapshot: DynamicScheduleDecisionSnapshot,
    )

    suspend fun persistTrace(
        trace: DynamicScheduleDecisionTraceRecord,
    )
}
