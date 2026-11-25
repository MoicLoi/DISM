package me.moiclathy.dism.schedule.persistence.sqldelight

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.db.ScheduleDecisionReplayInputQueries
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore

/**
 * 12.26A Replay Input Store 的 SQLDelight 实现。
 *
 * 分工：
 *
 * - `schedule_decision_replay_input` 保存 Runtime 真正收到的
 *   input / previousState / policy（深层对象图，canonical JSON）；
 * - `schedule_decision_snapshot` 保存决策摘要（结构化列）。
 *
 * 两者通过 `snapshot_id` 关联，
 * 读回时把 Snapshot 从 [snapshotStore] 取出来重新拼成完整对象，
 * 因此不会出现"同一份 Snapshot 存在两份、并且不一致"的情况。
 *
 * 这也是"重启后还能 replay / verify"的关键：
 * 只要这两个 Store 都落在 SQLite 上，
 * 进程重启后历史决策依然可以确定性重放。
 */
class SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore(
    private val queries: ScheduleDecisionReplayInputQueries,

    private val snapshotStore:
        DynamicScheduleDecisionSnapshotStore,
) : DynamicScheduleDecisionReplayInputSnapshotStore {

    constructor(
        database: DismDatabase,
        snapshotStore:
            DynamicScheduleDecisionSnapshotStore,
    ) : this(
        queries = database.scheduleDecisionReplayInputQueries,
        snapshotStore = snapshotStore,
    )

    override suspend fun save(
        snapshot:
            DynamicScheduleDecisionReplayInputSnapshot,
    ) {
        write(snapshot)
    }

    /**
     * 13.6：不开事务的写入，供
     * [SqlDelightDynamicScheduleDecisionAtomicPersistence]
     * 放进跨表 transaction。
     */
    internal fun write(
        snapshot:
            DynamicScheduleDecisionReplayInputSnapshot,
    ) {
        queries.insertOrReplace(
            snapshot_id =
                snapshot.decisionSnapshot.id.value.toString(),

            item_id =
                snapshot.decisionSnapshot.itemId.value,

            evaluated_at_epoch_ms =
                snapshot.evaluatedAt.toEpochMilliseconds(),

            schedule_revision = snapshot.scheduleRevision,

            engine_version = snapshot.engineVersion,

            payload_format_version =
                ScheduleDecisionPersistenceJson.VERSION,

            payload_json =
                ScheduleDecisionSqlDelightCodec
                    .encodeReplayInputPayload(
                        input = snapshot.input,

                        previousState =
                            snapshot.previousState,

                        policy = snapshot.policy,
                    ),
        )
    }

    override suspend fun get(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionReplayInputSnapshot? {

        val row =
            queries
                .selectById(snapshotId.value.toString())
                .executeAsOneOrNull()
                ?: return null

        require(
            row.payload_format_version ==
                ScheduleDecisionPersistenceJson.VERSION,
        ) {
            "Unsupported replay input payload version: " +
                row.payload_format_version
        }

        val decisionSnapshot =
            snapshotStore.get(snapshotId)
                ?: return null

        val payload =
            ScheduleDecisionSqlDelightCodec
                .decodeReplayInputPayload(row.payload_json)

        return DynamicScheduleDecisionReplayInputSnapshot(
            decisionSnapshot = decisionSnapshot,
            input = payload.input,
            previousState = payload.previousState,
            policy = payload.policy,
            evaluatedAt =
                kotlin.time.Instant.fromEpochMilliseconds(
                    row.evaluated_at_epoch_ms,
                ),
            scheduleRevision = row.schedule_revision,
            engineVersion = row.engine_version,
        )
    }

    /**
     * 13.9：枚举所有 ReplayInput 的父 Snapshot id。
     *
     * 只读主键列，**不解码 payload**：
     *
     * - 孤儿检测根本不需要 payload；
     * - 一行损坏的 JSON 不应该让整个枚举抛异常，
     *   否则"发现孤儿"这件事会被一条坏数据彻底挡住。
     */
    override suspend fun allSnapshotIds():
        List<DynamicScheduleDecisionSnapshotId> =
        queries
            .selectAllIds()
            .executeAsList()
            .map { id ->
                DynamicScheduleDecisionSnapshotId(
                    kotlin.uuid.Uuid.parse(id),
                )
            }

    override suspend fun delete(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ) {
        queries.deleteById(snapshotId.value.toString())
    }

    override suspend fun clear() {
        queries.deleteAll()
    }
}
