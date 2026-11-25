package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 13.6
 *
 * 一次 Decision 历史记录的**原子持久化边界**。
 *
 * 一个 Decision 由三部分组成：
 *
 * ```text
 * Decision D
 *  │
 *  ├── Snapshot(D)      "当时系统怎么看"
 *  ├── Trace(D)         "为什么这么判断"
 *  └── ReplayInput(D)   "能不能重新运行当时的决策"
 * ```
 *
 * 这三者不是三个独立的数据库对象，
 * 而是**一个 Decision Aggregate 的三个持久化投影**。
 * 所以它们必须作为一个持久化单元：
 *
 * ```text
 * 成功：Snapshot ✓  Trace ✓  ReplayInput ✓
 * 失败：Snapshot ✗  Trace ✗  ReplayInput ✗
 * ```
 *
 * 13.4 的恢复容错保证了"读的时候半截数据不会让 App 起不来"，
 * 13.6 保证的是**根本不会写出半截数据**。
 *
 * 注意接口里没有 SQLDelight：
 *
 * ```text
 * runtime → AtomicPersistencePort → { InMemory | SQLDelight }
 * ```
 */
interface DynamicScheduleDecisionAtomicPersistence {

    suspend fun persist(
        trace: DynamicScheduleDecisionTraceRecord,

        snapshot: DynamicScheduleDecisionSnapshot,

        replayInput:
            DynamicScheduleDecisionReplayInputSnapshot,
    )
}
