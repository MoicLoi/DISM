package me.moiclathy.dism.schedule.snapshot

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleDecisionSnapshotPersistenceService(
    private val store:
        DynamicScheduleDecisionSnapshotStore,
) {

    suspend fun persist(
        snapshot: DynamicScheduleDecisionSnapshot,
    ) {
        store.save(snapshot)
    }

    suspend fun find(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionSnapshot? {
        return store.get(snapshotId)
    }

    /**
     * 13.3：只读最近一次，不经过 [query] + maxBy。
     *
     * 这样 SQLDelight 的 `LIMIT 1` 才真正生效。
     */
    suspend fun latest(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshot? {
        return store.findLatestByItemId(itemId)
    }

    /**
     * 16.32：一批条目的"最近一次"。
     *
     * 见 [DynamicScheduleDecisionSnapshotStore.findLatestByItemIds]
     * ——那里解释了为什么需要它（消除每次启动 81ms 的 N+1）。
     */
    suspend fun latestForItems(
        itemIds: Collection<DynamicScheduleItemId>,
    ): List<DynamicScheduleDecisionSnapshot> {
        return store.findLatestByItemIds(itemIds)
    }

    suspend fun delete(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ) {
        store.delete(snapshotId)
    }

    suspend fun clear() {
        store.clear()
    }

    suspend fun query(
        query: DynamicScheduleDecisionSnapshotQuery,
    ): DynamicScheduleDecisionSnapshotQueryResult {
        return store.query(query)
    }
}
