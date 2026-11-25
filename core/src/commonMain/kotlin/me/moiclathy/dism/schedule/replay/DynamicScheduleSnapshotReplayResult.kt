package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot

data class DynamicScheduleSnapshotReplayResult(
    val snapshot:
        DynamicScheduleDecisionSnapshot,

    val originalPriority: Double,

    val currentPriority: Double,

    val originalAttentionRequired: Boolean,

    val currentAttentionRequired: Boolean,

    val priorityChanged: Boolean,

    val attentionDecisionChanged: Boolean,

    val conflictChanged: Boolean,

    val differences:
        List<DynamicScheduleSnapshotReplayDifference>,
) {

    val equivalent: Boolean
        get() = differences.isEmpty()
}
