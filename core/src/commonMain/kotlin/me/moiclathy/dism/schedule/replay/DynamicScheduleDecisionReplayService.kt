package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

interface DynamicScheduleDecisionReplayService {

    suspend fun replay(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionReplayReport?

    suspend fun replay(
        snapshotId: DynamicScheduleDecisionSnapshotId,
        currentPriority: Double,
        currentAttentionRequired: Boolean,
    ): DynamicScheduleDecisionReplayReport?
}
