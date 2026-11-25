package me.moiclathy.dism.schedule.persistence.sqldelight

import kotlin.time.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.Schedule_decision_provenance
import me.moiclathy.dism.persistence.db.ScheduleDecisionProvenanceQueries
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenance
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceId
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.provenance.DynamicScheduleReplayVerificationProvenance
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationStatus
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility

/**
 * 13.12.11
 *
 * Provenance 的 SQLDelight 实现。
 *
 * 与 Snapshot / Trace Store 一样：generated row 类型不泄漏到 domain，
 * row → domain 的转换留在本文件里。
 *
 * 13.12.10：故意**不建外键**。
 * Provenance 的生命周期与 Snapshot 不同——
 * Retention 删掉旧 Snapshot 时，Provenance 应当留下来，
 * 因为它是"历史解释索引"。
 */
@OptIn(ExperimentalUuidApi::class)
class SqlDelightDynamicScheduleDecisionProvenanceStore(
    private val queries: ScheduleDecisionProvenanceQueries,
) : DynamicScheduleDecisionProvenanceStore {

    constructor(
        database: DismDatabase,
    ) : this(database.scheduleDecisionProvenanceQueries)

    override suspend fun save(
        provenance:
            DynamicScheduleDecisionProvenance,
    ) {
        queries.insertOrReplace(
            provenance_id =
                provenance.id.value.toString(),

            item_id =
                provenance.itemId.value,

            snapshot_id =
                provenance.snapshotId.value.toString(),

            trace_id =
                provenance.traceId?.value?.toString(),

            replay_input_snapshot_id =
                provenance
                    .replayInputSnapshotId
                    ?.value
                    ?.toString(),

            historical_engine_version =
                provenance.historicalEngineVersion,

            replay_engine_version =
                provenance.replayEngineVersion,

            compatibility =
                provenance.compatibility.name,

            migration_id =
                provenance.migrationId,

            created_at_epoch_ms =
                provenance.createdAt.toEpochMilliseconds(),

            replayed_at_epoch_ms =
                provenance.replayedAt
                    ?.toEpochMilliseconds(),

            behavior_fingerprint =
                provenance.behaviorFingerprint,

            verified_at_epoch_ms =
                provenance
                    .verification
                    ?.verifiedAt
                    ?.toEpochMilliseconds(),

            verification_status =
                provenance
                    .verification
                    ?.status
                    ?.name,

            verification_historical_engine_version =
                provenance
                    .verification
                    ?.historicalEngineVersion,

            verification_engine_version =
                provenance
                    .verification
                    ?.replayEngineVersion,

            difference_count =
                provenance
                    .verification
                    ?.differenceCount
                    ?.toLong(),

            diagnostics_json =
                provenance
                    .verification
                    ?.diagnostics
                    ?.let(
                        DynamicScheduleReplayProvenanceCodec::encode,
                    ),
        )
    }

    override suspend fun get(
        id:
            DynamicScheduleDecisionProvenanceId,
    ): DynamicScheduleDecisionProvenance? =
        queries
            .selectById(id.value.toString())
            .executeAsOneOrNull()
            ?.let(::toDomain)

    override suspend fun latestForSnapshot(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionProvenance? =
        queries
            .selectLatestForSnapshot(
                snapshotId.value.toString(),
            )
            .executeAsOneOrNull()
            ?.let(::toDomain)

    override suspend fun queryForSnapshot(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): List<DynamicScheduleDecisionProvenance> =
        queries
            .selectForSnapshot(
                snapshotId.value.toString(),
            )
            .executeAsList()
            .map(::toDomain)

    override suspend fun all():
        List<DynamicScheduleDecisionProvenance> =
        queries
            .selectAll()
            .executeAsList()
            .map(::toDomain)

    override suspend fun queryForItem(
        itemId:
            DynamicScheduleItemId,
    ): List<DynamicScheduleDecisionProvenance> =
        queries
            .selectForItem(itemId.value)
            .executeAsList()
            .map(::toDomain)

    override suspend fun updateVerification(
        id:
            DynamicScheduleDecisionProvenanceId,

        verification:
            DynamicScheduleReplayVerificationProvenance,
    ) {
        queries.updateVerification(
            verified_at_epoch_ms =
                verification.verifiedAt.toEpochMilliseconds(),

            verification_status =
                verification.status.name,

            verification_historical_engine_version =
                verification.historicalEngineVersion,

            verification_engine_version =
                verification.replayEngineVersion,

            difference_count =
                verification.differenceCount.toLong(),

            diagnostics_json =
                DynamicScheduleReplayProvenanceCodec
                    .encode(verification.diagnostics),

            provenance_id = id.value.toString(),
        )
    }

    override suspend fun clear() {
        queries.deleteAll()
    }

    private fun toDomain(
        row: Schedule_decision_provenance,
    ): DynamicScheduleDecisionProvenance =
        DynamicScheduleDecisionProvenance(
            id =
                DynamicScheduleDecisionProvenanceId(
                    Uuid.parse(row.provenance_id),
                ),

            itemId =
                DynamicScheduleItemId(row.item_id),

            snapshotId =
                DynamicScheduleDecisionSnapshotId(
                    Uuid.parse(row.snapshot_id),
                ),

            traceId =
                row.trace_id?.let {
                    DynamicScheduleDecisionTraceId(
                        Uuid.parse(it),
                    )
                },

            replayInputSnapshotId =
                row.replay_input_snapshot_id?.let {
                    DynamicScheduleDecisionSnapshotId(
                        Uuid.parse(it),
                    )
                },

            historicalEngineVersion =
                row.historical_engine_version,

            replayEngineVersion =
                row.replay_engine_version,

            compatibility =
                parseCompatibility(row.compatibility),

            migrationId = row.migration_id,

            createdAt =
                Instant.fromEpochMilliseconds(
                    row.created_at_epoch_ms,
                ),

            replayedAt =
                row.replayed_at_epoch_ms?.let {
                    Instant.fromEpochMilliseconds(it)
                },

            behaviorFingerprint = row.behavior_fingerprint,

            verification = decodeVerification(row),
        )

    /**
     * 校验事实是**一组**字段。
     *
     * 只要状态存在就必须能读出完整记录；
     * 缺字段说明数据被外部改坏了，必须显式失败，
     * 而不是拼出一条 verifiedAt 为假的记录。
     */
    private fun decodeVerification(
        row: Schedule_decision_provenance,
    ): DynamicScheduleReplayVerificationProvenance? {

        val status =
            row.verification_status
                ?: return null

        return DynamicScheduleReplayVerificationProvenance(
            verifiedAt =
                Instant.fromEpochMilliseconds(
                    requireNotNull(row.verified_at_epoch_ms) {
                        "verification_status exists but verified_at is null"
                    },
                ),

            status =
                parseVerificationStatus(status),

            historicalEngineVersion =
                requireNotNull(
                    row.verification_historical_engine_version,
                ) {
                    "verification_status exists but historical engine version is null"
                },

            replayEngineVersion =
                requireNotNull(
                    row.verification_engine_version,
                ) {
                    "verification_status exists but verification engine version is null"
                },

            differenceCount =
                requireNotNull(row.difference_count) {
                    "verification_status exists but difference count is null"
                }.toInt(),

            diagnostics =
                DynamicScheduleReplayProvenanceCodec
                    .decode(row.diagnostics_json),
        )
    }

    private fun parseVerificationStatus(
        value: String,
    ): DynamicScheduleReplayVerificationStatus =
        runCatching {
            DynamicScheduleReplayVerificationStatus
                .valueOf(value)
        }.getOrElse {
            throw IllegalStateException(
                "Unknown verification status in database: $value",
                it,
            )
        }

    /**
     * 读到不认识的兼容性枚举值必须显式失败：
     * 数据库比代码新的时候，静默降级成别的状态
     * 会让"这条历史能不能重放"变成一个错误答案。
     */
    private fun parseCompatibility(
        value: String,
    ): DynamicScheduleHistoryVersionCompatibility =
        runCatching {
            DynamicScheduleHistoryVersionCompatibility
                .valueOf(value)
        }.getOrElse {
            throw IllegalStateException(
                "Unknown version compatibility in database: $value",
                it,
            )
        }
}
