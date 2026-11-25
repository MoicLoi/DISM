package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 12.26A
 *
 * 当前阶段使用 InMemory Store。
 *
 * 后续真正接入 SQLDelight 时，
 * 只需要替换 Store 实现，
 * Replay Input 模型本身不需要改变。
 */
class InMemoryDynamicScheduleDecisionReplayInputSnapshotStore :
    DynamicScheduleDecisionReplayInputSnapshotStore {

    private val snapshots =
        mutableMapOf<
            DynamicScheduleDecisionSnapshotId,
            DynamicScheduleDecisionReplayInputSnapshot,
        >()

    override suspend fun save(
        snapshot:
            DynamicScheduleDecisionReplayInputSnapshot,
    ) {
        snapshots[
            snapshot.decisionSnapshot.id
        ] = snapshot
    }

    override suspend fun get(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionReplayInputSnapshot? {
        return snapshots[snapshotId]
    }

    /**
     * 13.9：孤儿检测需要看到**所有**条目，
     * 包括父 Snapshot 已经消失的那些。
     *
     * 所以这里枚举的是存储 key，
     * 而不是"能拼出完整对象的条目"。
     */
    override suspend fun allSnapshotIds():
        List<DynamicScheduleDecisionSnapshotId> =
        snapshots.keys.toList()

    override suspend fun delete(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ) {
        snapshots.remove(snapshotId)
    }

    override suspend fun clear() {
        snapshots.clear()
    }
}
