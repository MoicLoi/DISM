package me.moiclathy.dism.schedule.persistence.sqldelight

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.ScheduleDecisionSnapshotQueries
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQueryResult
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore

/**
 * 12.15 Decision Snapshot Store 的 SQLDelight 实现。
 *
 * 只依赖 generated Queries，不依赖 DismDatabase 本身，
 * 与 11.24 的 Journal adapter 保持同一约定。
 */
class SqlDelightDynamicScheduleDecisionSnapshotStore(
    private val queries: ScheduleDecisionSnapshotQueries,
) : DynamicScheduleDecisionSnapshotStore {

    constructor(
        database: DismDatabase,
    ) : this(database.scheduleDecisionSnapshotQueries)

    override suspend fun save(
        snapshot: DynamicScheduleDecisionSnapshot,
    ) {
        write(snapshot)
    }

    /**
     * 13.6：不开事务的写入，供
     * [SqlDelightDynamicScheduleDecisionAtomicPersistence]
     * 放进跨表 transaction。
     */
    internal fun write(
        snapshot: DynamicScheduleDecisionSnapshot,
    ) {
        val values =
            ScheduleDecisionSqlDelightCodec
                .bindSnapshot(snapshot)

        queries.insertOrReplace(
            snapshot_id = values.snapshotId,
            item_id = values.itemId,
            captured_at_epoch_ms = values.capturedAtEpochMs,
            schedule_revision = values.scheduleRevision,
            engine_version = values.engineVersion,
            trace_id = values.traceId,
            item_title = values.itemTitle,
            item_state = values.itemState,
            item_priority = values.itemPriority,
            item_confidence = values.itemConfidence,
            context_active_keys = values.contextActiveKeys,
            context_required_satisfied =
                values.contextRequiredSatisfied,
            context_excluded_satisfied =
                values.contextExcludedSatisfied,
            memory_global_factor = values.memoryGlobalFactor,
            memory_context_factor = values.memoryContextFactor,
            memory_adaptation_factor =
                values.memoryAdaptationFactor,
            dependency_required_satisfied =
                values.dependencyRequiredSatisfied,
            dependency_missing_ids =
                values.dependencyMissingIds,
            dependency_required_count =
                values.dependencyRequiredCount,
            policy_minimum_attention_priority =
                values.policyMinimumAttentionPriority,
            policy_minimum_attention_confidence =
                values.policyMinimumAttentionConfidence,
            policy_minimum_hard_constraint_priority =
                values.policyMinimumHardConstraintPriority,
            policy_explicit_request_priority =
                values.policyExplicitRequestPriority,
            policy_maximum_learned_suppression =
                values.policyMaximumLearnedSuppression,
        )
    }

    override suspend fun get(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionSnapshot? =
        queries
            .selectById(snapshotId.value.toString())
            .executeAsOneOrNull()
            ?.let(ScheduleDecisionSqlDelightCodec::toSnapshot)

    /**
     * 13.3：启动恢复只读一行。
     *
     * 对应 SQL 的 `ORDER BY captured_at DESC LIMIT 1`，
     * 不会把整条历史加载进内存。
     */
    override suspend fun findLatestByItemId(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshot? =
        queries
            .selectLatestForItem(itemId.value)
            .executeAsOneOrNull()
            ?.let(ScheduleDecisionSqlDelightCodec::toSnapshot)

    /**
     * 16.32：一次查询取回一批条目的"最近一次"。
     *
     * ### 两处防御
     *
     * ```text
     * 空集合      直接返回，不走 SQL
     *             `IN ()` 在 SQLite 里是语法错误，
     *             而空集合是完全正常的一次调用
     *              （例如模型里一个条目都没有）
     * ```
     *
     * 注意 `selectLatestForItems` 在
     * `captured_at_epoch_ms` 并列时会为同一条目返回多行
     * （SQL 已经按 `snapshot_id DESC` 排好，较新的在前）。
     * 取每个 itemId 的第一条即得最近一次。
     */
    override suspend fun findLatestByItemIds(
        itemIds: Collection<DynamicScheduleItemId>,
    ): List<DynamicScheduleDecisionSnapshot> {

        if (itemIds.isEmpty()) {
            return emptyList()
        }

        val byItem =
            linkedMapOf<
                DynamicScheduleItemId,
                DynamicScheduleDecisionSnapshot,
                >()

        queries
            .selectLatestForItems(
                item_id = itemIds.map { it.value },
            )
            .executeAsList()
            .forEach { row ->
                val snapshot =
                    ScheduleDecisionSqlDelightCodec
                        .toSnapshot(row)

                /*
                 * 只保留每个 item 的第一条。
                 *
                 * SQL 已按 item_id ASC, snapshot_id DESC 排序，
                 * 所以第一条就是最近一次。
                 *
                 * ### 为什么不是 `putIfAbsent`
                 *
                 * 它在 JVM 上能编译（`kotlin.collections
                 * .LinkedHashMap` 在那里就是
                 * `java.util.LinkedHashMap`），但它是
                 * `java.util.Map` 的默认方法，**common 里不存在**。
                 *
                 * 这个项目已经因为同类问题吃过一次亏
                 * （`toSortedSet` 只在 JVM 上有）。写 KMP 的
                 * common 代码时，凡是"在 JVM 上顺理成章"的方法
                 * 都要先确认它在 common 里是否存在。
                 */
                if (!byItem.containsKey(snapshot.itemId)) {
                    byItem[snapshot.itemId] = snapshot
                }
            }

        return byItem.values.toList()
    }

    override suspend fun delete(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ) {
        queries.deleteById(snapshotId.value.toString())
    }

    override suspend fun clear() {
        queries.deleteAll()
    }

    override suspend fun query(
        query: DynamicScheduleDecisionSnapshotQuery,
    ): DynamicScheduleDecisionSnapshotQueryResult {

        val itemId = query.itemId?.value

        val from = query.from?.toEpochMilliseconds()

        val until = query.until?.toEpochMilliseconds()

        val snapshots =
            queries
                .selectFiltered(
                    item_id = itemId,
                    from_epoch_ms = from,
                    until_epoch_ms = until,
                    schedule_revision = query.scheduleRevision,
                    engine_version = query.engineVersion,
                    limit = query.limit.toLong(),
                )
                .executeAsList()
                .map(ScheduleDecisionSqlDelightCodec::toSnapshot)

        val totalMatched =
            queries
                .countFiltered(
                    item_id = itemId,
                    from_epoch_ms = from,
                    until_epoch_ms = until,
                    schedule_revision = query.scheduleRevision,
                    engine_version = query.engineVersion,
                )
                .executeAsOne()
                .toInt()

        return DynamicScheduleDecisionSnapshotQueryResult(
            snapshots = snapshots,
            totalMatched = totalMatched,
        )
    }
}
