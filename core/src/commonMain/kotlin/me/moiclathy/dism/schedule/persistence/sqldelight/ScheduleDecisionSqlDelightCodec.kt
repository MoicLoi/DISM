package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlin.time.Instant
import kotlin.uuid.Uuid

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer

import me.moiclathy.dism.persistence.db.Schedule_decision_snapshot
import me.moiclathy.dism.persistence.db.Schedule_decision_trace
import me.moiclathy.dism.persistence.db.Schedule_decision_trace_entry
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleAttentionMemorySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleContextSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDependencySnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleItemSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicSchedulePolicySnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTrace
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceEntry
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage

/**
 * SQLDelight row 与 12.x Decision Snapshot / Trace / Replay Input 之间的转换。
 *
 * 约定与既有 adapter 保持一致：
 *
 * - 数据库只保存稳定 primitive（TEXT / INTEGER / REAL）；
 * - 列表与 map 用 canonical JSON 存 TEXT，不用自定义分隔符
 *   （避免 context key / dependency id 里出现分隔符时的边界问题）；
 * - 布尔统一存 0 / 1；
 * - 时间统一存 epoch milliseconds，方便 SQL 侧做范围查询。
 */
object ScheduleDecisionSqlDelightCodec {

    private val stringListSerializer =
        ListSerializer(String.serializer())

    private val stringMapSerializer =
        MapSerializer(
            String.serializer(),
            String.serializer(),
        )

    // ---------------------------------------------------------------
    // 通用 primitive 编解码
    // ---------------------------------------------------------------

    fun encodeStringList(
        values: Collection<String>,
    ): String =
        ScheduleDecisionPersistenceJson.instance
            .encodeToString(
                stringListSerializer,
                values.toList().sorted(),
            )

    fun decodeStringList(
        value: String,
    ): List<String> =
        ScheduleDecisionPersistenceJson.instance
            .decodeFromString(
                stringListSerializer,
                value,
            )

    fun encodeStringMap(
        values: Map<String, String>,
    ): String =
        ScheduleDecisionPersistenceJson.instance
            .encodeToString(
                stringMapSerializer,
                values.toSortedMapDeterministically(),
            )

    fun decodeStringMap(
        value: String,
    ): Map<String, String> =
        ScheduleDecisionPersistenceJson.instance
            .decodeFromString(
                stringMapSerializer,
                value,
            )

    private fun Map<String, String>.toSortedMapDeterministically():
        Map<String, String> =
        entries
            .sortedBy { it.key }
            .associate { it.key to it.value }

    // ---------------------------------------------------------------
    // Decision Snapshot
    // ---------------------------------------------------------------

    fun toSnapshot(
        row: Schedule_decision_snapshot,
    ): DynamicScheduleDecisionSnapshot =
        DynamicScheduleDecisionSnapshot(
            id =
                DynamicScheduleDecisionSnapshotId(
                    Uuid.parse(row.snapshot_id),
                ),

            itemId =
                DynamicScheduleItemId(row.item_id),

            capturedAt =
                Instant.fromEpochMilliseconds(
                    row.captured_at_epoch_ms,
                ),

            scheduleRevision = row.schedule_revision,

            engineVersion = row.engine_version,

            item =
                DynamicScheduleItemSnapshot(
                    itemId =
                        DynamicScheduleItemId(row.item_id),

                    title = row.item_title,

                    state =
                        DynamicScheduleState
                            .valueOf(row.item_state),

                    priority = row.item_priority,

                    confidence = row.item_confidence,
                ),

            context =
                DynamicScheduleContextSnapshot(
                    activeContextKeys =
                        decodeStringList(
                            row.context_active_keys,
                        ).toSet(),

                    requiredSatisfied =
                        row.context_required_satisfied != 0L,

                    excludedSatisfied =
                        row.context_excluded_satisfied != 0L,
                ),

            attentionMemory =
                DynamicScheduleAttentionMemorySnapshot(
                    globalFactor = row.memory_global_factor,

                    contextFactor = row.memory_context_factor,

                    adaptationFactor =
                        row.memory_adaptation_factor,
                ),

            dependency =
                DynamicScheduleDependencySnapshot(
                    requiredSatisfied =
                        row.dependency_required_satisfied != 0L,

                    missingDependencyIds =
                        decodeStringList(
                            row.dependency_missing_ids,
                        ),

                    requiredDependencyCount =
                        row.dependency_required_count.toInt(),
                ),

            policy =
                DynamicSchedulePolicySnapshot(
                    minimumAttentionPriority =
                        row.policy_minimum_attention_priority,

                    minimumAttentionConfidence =
                        row.policy_minimum_attention_confidence,

                    minimumHardConstraintPriority =
                        row.policy_minimum_hard_constraint_priority,

                    explicitRequestPriority =
                        row.policy_explicit_request_priority,

                    maximumLearnedSuppression =
                        row.policy_maximum_learned_suppression,
                ),

            traceId =
                row.trace_id?.let {
                    DynamicScheduleDecisionTraceId(
                        Uuid.parse(it),
                    )
                },
        )

    fun bindSnapshot(
        snapshot: DynamicScheduleDecisionSnapshot,
    ): SqlDelightSnapshotValues =
        SqlDelightSnapshotValues(
            snapshotId = snapshot.id.value.toString(),

            itemId = snapshot.itemId.value,

            capturedAtEpochMs =
                snapshot.capturedAt.toEpochMilliseconds(),

            scheduleRevision = snapshot.scheduleRevision,

            engineVersion = snapshot.engineVersion,

            traceId = snapshot.traceId?.value?.toString(),

            itemTitle = snapshot.item.title,

            itemState = snapshot.item.state.name,

            itemPriority = snapshot.item.priority,

            itemConfidence = snapshot.item.confidence,

            contextActiveKeys =
                encodeStringList(
                    snapshot.context.activeContextKeys,
                ),

            contextRequiredSatisfied =
                snapshot.context.requiredSatisfied.toLong(),

            contextExcludedSatisfied =
                snapshot.context.excludedSatisfied.toLong(),

            memoryGlobalFactor =
                snapshot.attentionMemory.globalFactor,

            memoryContextFactor =
                snapshot.attentionMemory.contextFactor,

            memoryAdaptationFactor =
                snapshot.attentionMemory.adaptationFactor,

            dependencyRequiredSatisfied =
                snapshot.dependency.requiredSatisfied.toLong(),

            dependencyMissingIds =
                encodeStringList(
                    snapshot.dependency.missingDependencyIds,
                ),

            dependencyRequiredCount =
                snapshot.dependency
                    .requiredDependencyCount
                    .toLong(),

            policyMinimumAttentionPriority =
                snapshot.policy.minimumAttentionPriority,

            policyMinimumAttentionConfidence =
                snapshot.policy.minimumAttentionConfidence,

            policyMinimumHardConstraintPriority =
                snapshot.policy
                    .minimumHardConstraintPriority,

            policyExplicitRequestPriority =
                snapshot.policy.explicitRequestPriority,

            policyMaximumLearnedSuppression =
                snapshot.policy.maximumLearnedSuppression,
        )

    // ---------------------------------------------------------------
    // Trace
    // ---------------------------------------------------------------

    fun toTraceRecord(
        row: Schedule_decision_trace,
        entryRows: List<Schedule_decision_trace_entry>,
    ): DynamicScheduleDecisionTraceRecord =
        DynamicScheduleDecisionTraceRecord(
            trace =
                DynamicScheduleDecisionTrace(
                    id =
                        DynamicScheduleDecisionTraceId(
                            Uuid.parse(row.trace_id),
                        ),

                    itemId =
                        DynamicScheduleItemId(row.item_id),

                    entries =
                        entryRows
                            .sortedBy { it.ordinal }
                            .map(::toTraceEntry),
                ),

            createdAt =
                Instant.fromEpochMilliseconds(
                    row.created_at_epoch_ms,
                ),

            scheduleRevision = row.schedule_revision,

            engineVersion = row.engine_version,
        )

    fun toTraceEntry(
        row: Schedule_decision_trace_entry,
    ): DynamicScheduleDecisionTraceEntry =
        DynamicScheduleDecisionTraceEntry(
            stage =
                parseStage(row.stage),

            message = row.message,

            values =
                decodeStringMap(row.values_json),

            decisive = row.decisive != 0L,
        )

    fun bindTraceRecord(
        record: DynamicScheduleDecisionTraceRecord,
    ): SqlDelightTraceValues =
        SqlDelightTraceValues(
            traceId = record.traceId.value.toString(),

            itemId = record.itemId.value,

            createdAtEpochMs =
                record.createdAt.toEpochMilliseconds(),

            scheduleRevision = record.scheduleRevision,

            engineVersion = record.engineVersion,
        )

    fun bindTraceEntries(
        record: DynamicScheduleDecisionTraceRecord,
    ): List<SqlDelightTraceEntryValues> =
        record.trace.entries.mapIndexed { ordinal, entry ->
            SqlDelightTraceEntryValues(
                /*
                 * entry 自身没有 id，
                 * 用 (traceId, ordinal) 组成稳定主键，
                 * 这样重复 save 同一条 trace 是幂等的。
                 */
                entryId =
                    record.traceId.value.toString() +
                        "#" +
                        ordinal,

                traceId = record.traceId.value.toString(),

                ordinal = ordinal.toLong(),

                stage = entry.stage.name,

                message = entry.message,

                valuesJson =
                    encodeStringMap(entry.values),

                decisive = entry.decisive.toLong(),
            )
        }

    /**
     * Trace stage 是 12.14 的枚举。
     *
     * 数据库里存 name；读到未知值说明
     * 数据库比当前代码新，必须显式失败，
     * 而不是静默降级成别的 stage。
     */
    private fun parseStage(
        value: String,
    ): DynamicScheduleDecisionTraceStage =
        runCatching {
            DynamicScheduleDecisionTraceStage.valueOf(value)
        }.getOrElse {
            throw IllegalStateException(
                "Unknown trace stage in database: $value",
                it,
            )
        }

    // ---------------------------------------------------------------
    // Replay Input
    // ---------------------------------------------------------------

    /**
     * 载荷类型是 persistence 内部实现细节，
     * 因此编解码函数也是 internal：
     *
     * 上层只能通过 ReplayInput Store 端口读写，
     * 拿不到"半解码"的中间结构。
     */
    internal fun encodeReplayInputPayload(
        input:
            me.moiclathy.dism.schedule.runtime
                .DynamicScheduleIntelligenceInput,

        previousState:
            DynamicScheduleIntelligenceRuntimeState,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy,
    ): String =
        ScheduleDecisionPersistenceJson.instance
            .encodeToString(
                DynamicScheduleReplayInputPayload
                    .serializer(),
                DynamicScheduleReplayInputPayload(
                    input = input,
                    previousState = previousState,
                    policy = policy,
                ),
            )

    internal fun decodeReplayInputPayload(
        value: String,
    ): DynamicScheduleReplayInputPayload =
        ScheduleDecisionPersistenceJson.instance
            .decodeFromString(
                DynamicScheduleReplayInputPayload
                    .serializer(),
                value,
            )

    private fun Boolean.toLong(): Long =
        if (this) 1L else 0L
}

/**
 * `schedule_decision_snapshot` 一行的绑定值。
 */
@Serializable
data class SqlDelightSnapshotValues(
    val snapshotId: String,
    val itemId: String,
    val capturedAtEpochMs: Long,
    val scheduleRevision: Long,
    val engineVersion: String,
    val traceId: String?,
    val itemTitle: String,
    val itemState: String,
    val itemPriority: Double,
    val itemConfidence: Double,
    val contextActiveKeys: String,
    val contextRequiredSatisfied: Long,
    val contextExcludedSatisfied: Long,
    val memoryGlobalFactor: Double,
    val memoryContextFactor: Double,
    val memoryAdaptationFactor: Double,
    val dependencyRequiredSatisfied: Long,
    val dependencyMissingIds: String,
    val dependencyRequiredCount: Long,
    val policyMinimumAttentionPriority: Double,
    val policyMinimumAttentionConfidence: Double,
    val policyMinimumHardConstraintPriority: Double,
    val policyExplicitRequestPriority: Double,
    val policyMaximumLearnedSuppression: Double,
)

/**
 * `schedule_decision_trace` 一行的绑定值。
 */
@Serializable
data class SqlDelightTraceValues(
    val traceId: String,
    val itemId: String,
    val createdAtEpochMs: Long,
    val scheduleRevision: Long,
    val engineVersion: String,
)

/**
 * `schedule_decision_trace_entry` 一行的绑定值。
 */
@Serializable
data class SqlDelightTraceEntryValues(
    val entryId: String,
    val traceId: String,
    val ordinal: Long,
    val stage: String,
    val message: String,
    val valuesJson: String,
    val decisive: Long,
)
