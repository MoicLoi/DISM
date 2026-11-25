package me.moiclathy.dism.persistence.string

import kotlinx.serialization.json.Json

import me.moiclathy.dism.persistence.db.Schedule_decision_snapshot
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.persistence.sqldelight.ScheduleDecisionSqlDelightCodec
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightSnapshotValues
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQueryResult
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore

/**
 * 16.34：用**字符串存储 + JSON** 实现决策历史。
 *
 * ### 它解决的问题
 *
 * Web 上没有 SQLDelight driver，所以决策历史全在内存里：
 *
 * ```text
 * 刷新页面  →  日历还在（localStorage）
 *             历史清空（内存）
 * ```
 *
 * 这一层让历史也能跨刷新存活。
 *
 * ### 为什么它住在 commonMain
 *
 * 它只依赖 [StringStorage] 这个平台无关的接口，所以：
 *
 * ```text
 * JSON 编解码 + 查询语义   commonMain   可在 JVM 上完整测试
 * localStorage 绑定        webMain      很薄
 * ```
 *
 * 如果按"只有 Web 用它"的直觉把它放进 webMain，
 * 那它的每一条逻辑都只能靠浏览器验证——而当前
 * **没有任何浏览器自动化**，等于全部不可验证。
 *
 * ### 存储格式
 *
 * 复用 SQLDelight 那套扁平列（[SqlDelightSnapshotValues]），
 * 而不是另发明一种。理由：
 *
 * ```text
 * 编解码已在 ScheduleDecisionSqlDelightCodec 里有一份
 *   → 复用它可以避免"两份字段映射慢慢漂移"
 * 而且 SQLDelight 的两个实现（内存 / SQLite）
 *   与这里的语义能被同一批断言覆盖
 * ```
 *
 * 它的代价是把 SQLite 的列名当作 JSON 的键——
 * 名字不好看，但它换来了"映射只有一处"。
 *
 * ### 写入代价
 *
 * 每次写入都会重写整个数组。对**决策历史**这是可接受的：
 *
 * ```text
 * 它按"决策变化"追加，而不是每次加载都写（见 16.22）
 * 一个条目几十条历史，整个文件在几十 KB 量级
 * ```
 *
 * 如果哪天历史规模上来，这里应该换成分片键
 * （例如 `snapshot:<id>`），那是接口能支持的演进。
 */
class JsonDynamicScheduleDecisionSnapshotStore(
    private val storage: StringStorage,

    private val key: String = DEFAULT_KEY,
) : DynamicScheduleDecisionSnapshotStore {

    private val json =
        Json {
            /*
             * 未知字段要忽略而不是报错。
             *
             * 用户浏览器里可能留着旧版本写的数据；
             * 忽略未知字段让升级不会把历史读崩。
             */
            ignoreUnknownKeys = true
        }

    override suspend fun save(
        snapshot: DynamicScheduleDecisionSnapshot,
    ) {
        val values =
            ScheduleDecisionSqlDelightCodec.bindSnapshot(snapshot)

        val current = loadAll()

        /*
         * 按 id 覆盖而不是追加。
         *
         * `save` 的语义是 upsert（与 SQLDelight 的
         * `INSERT OR REPLACE` 一致），所以重复保存同一份
         * 快照不该产生两条记录。
         */
        val updated =
            current
                .filterNot { it.snapshotId == values.snapshotId }
                .plus(values)

        persist(updated)
    }

    override suspend fun get(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionSnapshot? =
        loadAll()
            .firstOrNull {
                it.snapshotId == snapshotId.value.toString()
            }
            ?.let(::toSnapshot)

    override suspend fun findLatestByItemId(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleDecisionSnapshot? =
        latestFor(listOf(itemId.value)).firstOrNull()
            ?.let(::toSnapshot)

    override suspend fun findLatestByItemIds(
        itemIds: Collection<DynamicScheduleItemId>,
    ): List<DynamicScheduleDecisionSnapshot> =
        latestFor(itemIds.map { it.value })
            .map(::toSnapshot)

    override suspend fun delete(
        snapshotId: DynamicScheduleDecisionSnapshotId,
    ) {
        val id = snapshotId.value.toString()

        persist(loadAll().filterNot { it.snapshotId == id })
    }

    override suspend fun clear() {
        storage.remove(key)
    }

    override suspend fun query(
        query: DynamicScheduleDecisionSnapshotQuery,
    ): DynamicScheduleDecisionSnapshotQueryResult {

        val matched =
            loadAll()
                .map(::toSnapshot)
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
                .sortedWith(NEWEST_FIRST)

        return DynamicScheduleDecisionSnapshotQueryResult(
            snapshots = matched.take(query.limit),
            totalMatched = matched.size,
        )
    }

    /**
     * 每个条目取最近一次。
     *
     * 语义与 SQL 版本的 `ORDER BY item_id ASC, snapshot_id DESC`
     * 对齐：先按条目分组取最新的，再按条目 id 排序。
     */
    private fun latestFor(
        itemIds: List<String>,
    ): List<SqlDelightSnapshotValues> {

        if (itemIds.isEmpty()) {
            return emptyList()
        }

        val wanted = itemIds.toSet()

        return loadAll()
            .filter { it.itemId in wanted }
            .groupBy { it.itemId }
            .map { (_, forItem) ->
                forItem.maxWith(
                    compareBy(
                        { it.capturedAtEpochMs },
                        { it.snapshotId },
                    ),
                )
            }
            .sortedBy { it.itemId }
    }

    private fun loadAll(): List<SqlDelightSnapshotValues> {
        val raw = storage.get(key) ?: return emptyList()

        return try {
            json.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(
                    SqlDelightSnapshotValues.serializer(),
                ),
                raw,
            )
        } catch (_: Exception) {
            /*
             * 解析失败时当作"没有历史"，而不是抛。
             *
             * 浏览器里可能留着不兼容的旧数据；让它把整个应用
             * 带崩（历史的读取发生在每次加载时）代价过大。
             * 历史丢了是可恢复的，应用起不来不是。
             */
            emptyList()
        }
    }

    private fun persist(
        values: List<SqlDelightSnapshotValues>,
    ) {
        storage.set(
            key,
            json.encodeToString(
                kotlinx.serialization.builtins.ListSerializer(
                    SqlDelightSnapshotValues.serializer(),
                ),
                values,
            ),
        )
    }

    private companion object {
        const val DEFAULT_KEY = "dism.schedule.decision.snapshots"

        /**
         * 与 SQL 的 `ORDER BY captured_at DESC, snapshot_id DESC` 一致。
         */
        val NEWEST_FIRST =
            compareByDescending<DynamicScheduleDecisionSnapshot> {
                it.capturedAt
            }.thenByDescending {
                it.id.value
            }
    }
}

/**
 * 扁平列 → 领域对象。
 *
 * 复用 SQLDelight 的编解码，途径是**构造生成的行类型**再交给它。
 *
 * 为什么绕这一圈而不是直接写一份映射：那样就有了两份字段映射，
 * 而它们会慢慢漂移（加了字段只改一处）。生成的行类型
 * 与 [SqlDelightSnapshotValues] 的字段顺序一致，
 * 所以这个构造是机械的。
 */
private fun toSnapshot(
    values: SqlDelightSnapshotValues,
): DynamicScheduleDecisionSnapshot =
    ScheduleDecisionSqlDelightCodec.toSnapshot(
        Schedule_decision_snapshot(
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
            dependency_missing_ids = values.dependencyMissingIds,
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
        ),
    )
