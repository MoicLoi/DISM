package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 12.26A
 *
 * Replay Input Persistence Service。
 *
 * Service 层负责稳定 API，
 * Store 层负责具体存储实现。
 */
class DynamicScheduleDecisionReplayInputSnapshotPersistenceService(
    private val store:
        DynamicScheduleDecisionReplayInputSnapshotStore,
) {

    suspend fun persist(
        snapshot:
            DynamicScheduleDecisionReplayInputSnapshot,
    ) {
        store.save(snapshot)
    }

    suspend fun find(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionReplayInputSnapshot? {
        return store.get(snapshotId)
    }

    suspend fun delete(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ) {
        store.delete(snapshotId)
    }

    suspend fun clear() {
        store.clear()
    }
}
