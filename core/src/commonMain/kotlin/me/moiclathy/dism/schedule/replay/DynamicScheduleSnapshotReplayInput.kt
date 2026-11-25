package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleAttentionMemorySnapshot
import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult

data class DynamicScheduleSnapshotReplayInput(
    val snapshot:
        DynamicScheduleDecisionSnapshot,

    val currentPriority: Double,

    val currentAttentionRequired: Boolean,

    val currentConflict:
        DynamicScheduleAttentionConflictResult? = null,
) {
    init {
        require(currentPriority in 0.0..1.0)
    }
}
