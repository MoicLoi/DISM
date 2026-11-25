package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

data class DynamicScheduleDecisionPersistenceResult(
    val snapshots:
        List<DynamicScheduleDecisionSnapshot>,

    val traces:
        List<DynamicScheduleDecisionTraceRecord>,

    /**
     * 12.26A：
     *
     * 与每个 Decision Snapshot 对应的完整历史 Runtime Input。
     *
     * 一个 schedule item 对应一个 replay input snapshot。
     */
    val replayInputs:
        List<DynamicScheduleDecisionReplayInputSnapshot> = emptyList(),
) {
    val count: Int
        get() = snapshots.size

    val replayInputCount: Int
        get() = replayInputs.size
}
