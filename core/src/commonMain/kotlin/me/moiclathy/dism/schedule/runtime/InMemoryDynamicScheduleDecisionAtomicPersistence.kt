package me.moiclathy.dism.schedule.runtime

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore

/**
 * 13.6
 *
 * InMemory 环境下的原子持久化。
 *
 * InMemory 当然没有数据库 transaction，
 * 但仍然提供 **rollback 语义**：
 *
 * ```text
 * 先记下三个对象原来的值
 *        ↓
 * 依次写入
 *        ↓
 * 任何一步失败 → 恢复原值 / 删除刚写的
 *        ↓
 * 重新抛出异常
 * ```
 *
 * 这样 commonTest 与 SQLDelight 走的是**同一个 Persistence Boundary**，
 * 而不是"内存测试永远成功、真库才出问题"。
 */
class InMemoryDynamicScheduleDecisionAtomicPersistence(
    private val snapshotStore:
        DynamicScheduleDecisionSnapshotStore,

    private val traceStore:
        DynamicScheduleDecisionTraceStore,

    private val replayInputStore:
        DynamicScheduleDecisionReplayInputSnapshotStore,
) : DynamicScheduleDecisionAtomicPersistence {

    override suspend fun persist(
        trace: DynamicScheduleDecisionTraceRecord,

        snapshot: DynamicScheduleDecisionSnapshot,

        replayInput:
            DynamicScheduleDecisionReplayInputSnapshot,
    ) {

        val previousTrace =
            traceStore.get(trace.trace.id)

        val previousSnapshot =
            snapshotStore.get(snapshot.id)

        val previousReplayInput =
            replayInputStore.get(snapshot.id)

        try {

            traceStore.save(trace)

            snapshotStore.save(snapshot)

            replayInputStore.save(replayInput)

        } catch (failure: Throwable) {

            rollback(
                trace = trace,
                snapshot = snapshot,
                previousTrace = previousTrace,
                previousSnapshot = previousSnapshot,
                previousReplayInput = previousReplayInput,
            )

            throw failure
        }
    }

    /**
     * 补偿式回滚。
     *
     * 关键点：**每一步都是 best-effort**。
     *
     * 设计稿里的版本直接依次调用 `save/delete`，
     * 但回滚时面对的往往正是"刚刚失败的那个 Store"——
     * 于是回滚自己又抛异常，后面两步根本不会执行，
     * 结果留下半截数据（这正是 13.6 要消灭的东西）。
     *
     * 所以这里：
     *
     * ```text
     * 每一步独立 try
     *        ↓
     * 一步失败不影响后面两步
     *        ↓
     * 重新抛出**原始**异常
     * ```
     *
     * 回滚失败的细节不覆盖原始原因：
     * 调用方关心的是"这次决策没写成功"，
     * 而不是补偿过程中又发生了什么。
     */
    private suspend fun rollback(
        trace: DynamicScheduleDecisionTraceRecord,

        snapshot: DynamicScheduleDecisionSnapshot,

        previousTrace:
            DynamicScheduleDecisionTraceRecord?,

        previousSnapshot:
            DynamicScheduleDecisionSnapshot?,

        previousReplayInput:
            DynamicScheduleDecisionReplayInputSnapshot?,
    ) {

        /*
         * 逆序回滚：ReplayInput → Snapshot → Trace。
         */

        runCatching {

            if (previousReplayInput != null) {

                replayInputStore.save(previousReplayInput)

            } else {

                replayInputStore.delete(snapshot.id)
            }
        }

        runCatching {

            if (previousSnapshot != null) {

                snapshotStore.save(previousSnapshot)

            } else {

                snapshotStore.delete(snapshot.id)
            }
        }

        runCatching {

            if (previousTrace != null) {

                traceStore.save(previousTrace)

            } else {

                traceStore.delete(trace.trace.id)
            }
        }
    }
}
