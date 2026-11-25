package me.moiclathy.dism.persistence

import me.moiclathy.dism.api.DismInternalApi
import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.DatabaseDriverFactory
import me.moiclathy.dism.persistence.repository.SqlDelightEntityRepository
import me.moiclathy.dism.persistence.repository.SqlDelightEvidenceRepository
import me.moiclathy.dism.persistence.repository.SqlDelightRelationStateRepository
import me.moiclathy.dism.persistence.repository.SqlDelightTransitionLogRepository
import me.moiclathy.dism.persistence.repository.SqlDelightStateCommitRepository
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleAdaptationPersistenceStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleAdaptationReplayInputStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionAtomicPersistence
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.persistence.sqldelight.SqlDelightDynamicScheduleDecisionTraceStore

/**
 * 用平台 DatabaseDriverFactory 组装 SQLDelight 持久化容器。
 */
@DismInternalApi
class DismPersistenceFactory(
    private val driverFactory: DatabaseDriverFactory,
) {

    fun create(): DismPersistence {

        val database =
            DismDatabase(
                driverFactory.createDriver(),
            )

        /*
         * 12.x 的 Replay Input 需要按 snapshot_id 关联 Decision Snapshot，
         * 所以两个 Store 共用同一个数据库实例。
         */
        val scheduleSnapshots =
            SqlDelightDynamicScheduleDecisionSnapshotStore(
                database,
            )

        val scheduleTraces =
            SqlDelightDynamicScheduleDecisionTraceStore(
                database,
            )

        val scheduleReplayInputs =
            SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore(
                database = database,
                snapshotStore = scheduleSnapshots,
            )

        return DismPersistence(
            entities =
                SqlDelightEntityRepository(
                    database,
                ),
            evidence =
                SqlDelightEvidenceRepository(
                    database,
                ),
            relationStates =
                SqlDelightRelationStateRepository(
                    database,
                ),
            transitionLog =
                SqlDelightTransitionLogRepository(
                    database,
                ),
            stateCommit =
                SqlDelightStateCommitRepository(
                    database,
                ),
            scheduleSnapshots = scheduleSnapshots,
            scheduleTraces = scheduleTraces,
            scheduleReplayInputs = scheduleReplayInputs,

            scheduleProvenance =
                SqlDelightDynamicScheduleDecisionProvenanceStore(
                    database,
                ),

            /*
             * 13.6：原子边界必须和三个 Store 共用同一个 database，
             * 否则事务无法覆盖三张表。
             */
            scheduleDecisionAtomicPersistence =
                SqlDelightDynamicScheduleDecisionAtomicPersistence(
                    database = database,
                    snapshotStore = scheduleSnapshots,
                    traceStore = scheduleTraces,
                    replayInputStore = scheduleReplayInputs,
                ),

            /*
             * 14.11：Adaptive Decision 的长期持久化。
             *
             * 它不参与 13.x 的历史 Retention——
             * 适配记录是"当前有效状态"，不是历史证据。
             */
            scheduleAdaptations =
                SqlDelightDynamicScheduleAdaptationPersistenceStore(
                    database,
                ),

            /*
             * 14.12：重放输入与适配结果分开存，
             * 两者放在同一个 database 里以共享事务。
             */
            scheduleAdaptationReplayInputs =
                SqlDelightDynamicScheduleAdaptationReplayInputStore(
                    database,
                ),
        )
    }
}
