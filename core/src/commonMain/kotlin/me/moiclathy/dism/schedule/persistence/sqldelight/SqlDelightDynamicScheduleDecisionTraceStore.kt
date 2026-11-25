package me.moiclathy.dism.schedule.persistence.sqldelight

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.ScheduleDecisionTraceQueries
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQuery
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQueryResult
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore

/**
 * 12.15 Decision Trace Store 的 SQLDelight 实现。
 *
 * Trace 是"一次决策为什么这样"的证据链，
 * 所以 entry 单独一张表，按 ordinal 排序读回，
 * 保证与写入时的顺序完全一致。
 */
class SqlDelightDynamicScheduleDecisionTraceStore(
    private val queries: ScheduleDecisionTraceQueries,
) : DynamicScheduleDecisionTraceStore {

    constructor(
        database: DismDatabase,
    ) : this(database.scheduleDecisionTraceQueries)

    override suspend fun save(
        record: DynamicScheduleDecisionTraceRecord,
    ) {
        /*
         * Trace 与 Entry 必须一起成功或一起失败，
         * 否则可能出现"有 Trace 但没有 Entry"的半截历史。
         */
        queries.transaction {
            write(record)
        }
    }

    /**
     * 13.6：
     *
     * 不开自己的 transaction，写入直接归属调用方的事务。
     *
     * [SqlDelightDynamicScheduleDecisionAtomicPersistence] 用它把
     * Trace / Snapshot / ReplayInput 放进同一个 SQLite transaction，
     * 同时**不复制任何映射逻辑**——
     * 否则以后加一列就会出现"两处映射不一致"的静默 bug。
     */
    internal fun write(
        record: DynamicScheduleDecisionTraceRecord,
    ) {
        val traceValues =
            ScheduleDecisionSqlDelightCodec
                .bindTraceRecord(record)

        val entryValues =
            ScheduleDecisionSqlDelightCodec
                .bindTraceEntries(record)

        queries.insertOrReplaceTrace(
            trace_id = traceValues.traceId,
            item_id = traceValues.itemId,
            created_at_epoch_ms =
                traceValues.createdAtEpochMs,
            schedule_revision =
                traceValues.scheduleRevision,
            engine_version = traceValues.engineVersion,
        )

        /*
         * 同一条 trace 重复 save 时先清空旧 entry，
         * 避免 ordinal 变化后残留脏数据。
         */
        queries.deleteEntriesForTrace(
            traceValues.traceId,
        )

        entryValues.forEach { entry ->

            queries.insertOrReplaceEntry(
                entry_id = entry.entryId,
                trace_id = entry.traceId,
                ordinal = entry.ordinal,
                stage = entry.stage,
                message = entry.message,
                values_json = entry.valuesJson,
                decisive = entry.decisive,
            )
        }
    }

    override suspend fun get(
        traceId: DynamicScheduleDecisionTraceId,
    ): DynamicScheduleDecisionTraceRecord? =
        readRecord(traceId.value.toString())

    /**
     * 13.3：Trace 不再提供 "按 item 找最近"。
     *
     * 启动恢复走 Snapshot 的 traceId：
     *
     * ```text
     * snapshotStore.findLatestByItemId(itemId)
     *        │
     *        ▼ traceId
     * traceStore.get(traceId)
     * ```
     */
    override suspend fun delete(
        traceId: DynamicScheduleDecisionTraceId,
    ) {
        queries.transaction {
            queries.deleteEntriesForTrace(
                traceId.value.toString(),
            )

            queries.deleteTraceById(
                traceId.value.toString(),
            )
        }
    }

    override suspend fun clear() {
        queries.transaction {
            queries.deleteAllEntries()
            queries.deleteAllTraces()
        }
    }

    override suspend fun query(
        query: DynamicScheduleDecisionTraceQuery,
    ): DynamicScheduleDecisionTraceQueryResult {

        val itemId = query.itemId?.value

        val from = query.from?.toEpochMilliseconds()

        val until = query.until?.toEpochMilliseconds()

        val stage = query.stage?.name

        val decisiveOnly =
            if (query.decisiveOnly) 1L else 0L

        val records =
            queries
                .selectFiltered(
                    item_id = itemId,
                    from_epoch_ms = from,
                    until_epoch_ms = until,
                    stage = stage,
                    decisive_only = decisiveOnly,
                    limit = query.limit.toLong(),
                )
                .executeAsList()
                .mapNotNull { row ->
                    readRecord(row.trace_id)
                }

        val totalMatched =
            queries
                .countFiltered(
                    item_id = itemId,
                    from_epoch_ms = from,
                    until_epoch_ms = until,
                    stage = stage,
                    decisive_only = decisiveOnly,
                )
                .executeAsOne()
                .toInt()

        return DynamicScheduleDecisionTraceQueryResult(
            records = records,
            totalMatched = totalMatched,
        )
    }

    private fun readRecord(
        traceId: String,
    ): DynamicScheduleDecisionTraceRecord? {

        val row =
            queries
                .selectTraceById(traceId)
                .executeAsOneOrNull()
                ?: return null

        val entries =
            queries
                .selectEntriesForTrace(traceId)
                .executeAsList()

        return ScheduleDecisionSqlDelightCodec
            .toTraceRecord(
                row = row,
                entryRows = entries,
            )
    }
}
