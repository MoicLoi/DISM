package me.moiclathy.dism.schedule.persistence.sqldelight

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.runtime.DynamicScheduleDecisionAtomicPersistence
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 13.6
 *
 * Snapshot + Trace + ReplayInput 的**真正 SQLDelight 原子持久化**。
 *
 * 三份数据（四张物理表）全部处于同一个 SQLite transaction：
 *
 * ```text
 * schedule_decision_trace
 * schedule_decision_trace_entry
 * schedule_decision_snapshot
 * schedule_decision_replay_input
 * ```
 *
 * ### 为什么主构造是三个写入函数
 *
 * 实现上刻意**复用三个 Store 的写入逻辑**，而不是在这里重抄一遍 insert：
 *
 * - 加一列时只需要改一处映射；
 * - 不会出现"Store 写了新列、Atomic 忘了写"这种静默不一致；
 * - 事务仍然只有一个（SQLDelight 的事务是 driver 级的）。
 *
 * 用函数而不是具体 Store 类型作为主构造，是为了让
 * "事务中途失败会怎样"这件事**可以被真实地测出来**——
 * 用三个具体 SQL Store 的话，无法在不改生产代码的前提下注入一次失败，
 * 而"事务能否回滚"恰恰是 13.6 唯一重要的性质。
 *
 * 常规装配请用接受 Store 的次构造。
 */
class SqlDelightDynamicScheduleDecisionAtomicPersistence(
    private val database: DismDatabase,

    private val writeSnapshot:
        (DynamicScheduleDecisionSnapshot) -> Unit,

    private val writeTrace:
        (DynamicScheduleDecisionTraceRecord) -> Unit,

    private val writeReplayInput:
        (DynamicScheduleDecisionReplayInputSnapshot) -> Unit,
) : DynamicScheduleDecisionAtomicPersistence {

    constructor(
        database: DismDatabase,

        snapshotStore:
            SqlDelightDynamicScheduleDecisionSnapshotStore,

        traceStore:
            SqlDelightDynamicScheduleDecisionTraceStore,

        replayInputStore:
            SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore,
    ) : this(
        database = database,
        writeSnapshot = snapshotStore::write,
        writeTrace = traceStore::write,
        writeReplayInput = replayInputStore::write,
    )

    override suspend fun persist(
        trace: DynamicScheduleDecisionTraceRecord,

        snapshot: DynamicScheduleDecisionSnapshot,

        replayInput:
            DynamicScheduleDecisionReplayInputSnapshot,
    ) {

        /*
         * 顺序与 13.6 的语义一致：
         *
         * Trace → Snapshot → ReplayInput
         *
         * 中途任何一步抛异常，整个 SQLite transaction 回滚，
         * 四张表一个字节都不会留下。
         */
        database.transaction {

            writeTrace(trace)

            writeSnapshot(snapshot)

            writeReplayInput(replayInput)
        }
    }
}
