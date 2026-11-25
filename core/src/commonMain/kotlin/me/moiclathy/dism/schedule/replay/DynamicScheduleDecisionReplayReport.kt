package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

data class DynamicScheduleDecisionReplayReport(
    val snapshot:
        DynamicScheduleDecisionSnapshot,

    val originalTrace:
        DynamicScheduleDecisionTraceRecord?,

    val originalPriority: Double,

    val currentPriority: Double,

    val originalAttentionRequired: Boolean,

    val currentAttentionRequired: Boolean,

    val severity:
        DynamicScheduleDecisionReplaySeverity,

    val changedFactors:
        List<DynamicScheduleDecisionReplayChangedFactor>,

    val differences:
        List<DynamicScheduleSnapshotReplayDifference>,

    val summary: String,
) {
    init {
        require(originalPriority in 0.0..1.0)
        require(currentPriority in 0.0..1.0)
        require(summary.isNotBlank())
    }

    val equivalent: Boolean
        get() = severity ==
            DynamicScheduleDecisionReplaySeverity.NONE

    val behaviorChanged: Boolean
        get() =
            originalAttentionRequired !=
                currentAttentionRequired
}
