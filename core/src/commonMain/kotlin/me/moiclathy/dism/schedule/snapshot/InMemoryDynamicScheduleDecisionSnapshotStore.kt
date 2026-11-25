package me.moiclathy.dism.schedule.snapshot

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class InMemoryDynamicScheduleDecisionSnapshotStore :
    DynamicScheduleDecisionSnapshotStore {

    private val snapshots =
        LinkedHashMap<
            DynamicScheduleDecisionSnapshotId,
            DynamicScheduleDecisionSnapshot,
        >()

    override suspend fun save(
        snapshot: DynamicScheduleDecisionSnapshot,
    ) {
        snapshots[snapshot.id] = snapshot
    }

    override suspend fun get(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionSnapshot? {
        return snapshots[snapshotId]
    }

    override suspend fun findLatestByItemId(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshot? {

        return snapshots.values
            .asSequence()
            .filter {
                it.itemId == itemId
            }
            .maxByOrNull {
                it.capturedAt
            }
    }

    /**
     * 16.32：批量版本。
     *
     * ### 这里刻意**不**写成"循环调用单条版本"
     *
     * 在内存实现下循环版的结果与代价都一样小，于是看不出问题。
     * 但它会让这个端口在两个实现之间出现语义分歧：
     *
     * ```text
     * SQLDelight   一次查询
     * InMemory     一次遍历
     * 循环版        N 次遍历（内存里无所谓，但语义不同）
     * ```
     *
     * 更实际的理由：如果这里偷懒循环，将来有人拿内存实现
     * 做性能测量，会得出"没有 N+1"的错误结论。
     *
     * ### 并列时取较新的 snapshotId
     *
     * 与 SQL 版本的 `ORDER BY snapshot_id DESC` 对齐。
     *
     * 注意单条实现在并列时返回遍历顺序里的最后一个
     * （也就是插入顺序里的最后一个），而这里是按 id 取大。
     * 两者在 `capturedAt` 完全相同时可能不同——
     * 但那是毫秒时间戳且同一条目连写两条的情况，
     * 实践上不会发生。这里选择与 SQL 语义对齐，
     * 并把差异写出来而不是假装它不存在。
     */
    override suspend fun findLatestByItemIds(
        itemIds: Collection<DynamicScheduleItemId>,
    ): List<DynamicScheduleDecisionSnapshot> {

        if (itemIds.isEmpty()) {
            return emptyList()
        }

        val wanted = itemIds.toSet()

        return snapshots.values
            .asSequence()
            .filter { it.itemId in wanted }
            .groupBy { it.itemId }
            .map { (_, forItem) ->
                forItem.maxWith(
                    compareBy(
                        { it.capturedAt },
                        { it.id.value },
                    ),
                )
            }
            .sortedBy { it.itemId.value }
            .toList()
    }

    override suspend fun delete(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ) {
        snapshots.remove(snapshotId)
    }

    override suspend fun clear() {
        snapshots.clear()
    }

    override suspend fun query(
        query: DynamicScheduleDecisionSnapshotQuery,
    ): DynamicScheduleDecisionSnapshotQueryResult {

        val matched =
            snapshots.values
                .asSequence()
                .filter { snapshot ->
                    query.itemId == null ||
                        snapshot.itemId == query.itemId
                }
                .filter { snapshot ->
                    query.from == null ||
                        snapshot.capturedAt >= query.from
                }
                .filter { snapshot ->
                    query.until == null ||
                        snapshot.capturedAt <= query.until
                }
                .filter { snapshot ->
                    query.scheduleRevision == null ||
                        snapshot.scheduleRevision ==
                            query.scheduleRevision
                }
                .filter { snapshot ->
                    query.engineVersion == null ||
                        snapshot.engineVersion ==
                            query.engineVersion
                }
                .sortedByDescending {
                    it.capturedAt
                }
                .toList()

        return DynamicScheduleDecisionSnapshotQueryResult(
            snapshots =
                matched.take(query.limit),

            totalMatched =
                matched.size,
        )
    }
}
