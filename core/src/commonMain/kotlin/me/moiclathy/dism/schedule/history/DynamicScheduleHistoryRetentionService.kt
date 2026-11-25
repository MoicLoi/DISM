package me.moiclathy.dism.schedule.history

import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore

/**
 * 13.5
 *
 * 历史压缩 —— 不是 `clearHistory()`。
 *
 * DISM 的历史不是"三张表，过期一起删"，
 * 而是一张有引用关系的数据图：
 *
 * ```text
 * Snapshot ──traceId──► Trace
 * Snapshot ──snapshotId──► ReplayInput
 * ```
 *
 * 所以压缩顺序必须是：
 *
 * ```text
 * 找出要删的 Snapshot
 *        ↓
 * 记录它们的 traceId
 *        ↓
 * 删除 ReplayInput        （先删从属数据）
 *        ↓
 * 删除 Snapshot
 *        ↓
 * 确认 Trace 是否仍被引用
 *        ↓
 * 没有引用 → 删除 Trace
 * ```
 *
 * 由此保证三件事：
 *
 * 1. 最新一次决策（用户当前看到的那条）永远在；
 * 2. 不会留下"能重放但没有决策"或反之的孤儿数据；
 * 3. 共享的 Trace 不会因为一次压缩被误删。
 *
 * 边界（13.5.13）：
 * 这个 Service 是历史生命周期的**唯一协调者**。
 * 现阶段刻意不做 `DELETE WHERE captured_at < ...` 这类 SQL 批量优化——
 * 那是 13.6 的性能话题，不应该和领域边界搅在一起。
 */
class DynamicScheduleHistoryRetentionService(
    private val snapshotStore:
        DynamicScheduleDecisionSnapshotStore,

    private val traceStore:
        DynamicScheduleDecisionTraceStore,

    private val replayInputStore:
        DynamicScheduleDecisionReplayInputSnapshotStore,
) {

    suspend fun compact(
        itemIds: Collection<DynamicScheduleItemId>,

        now: Instant,

        policy: DynamicScheduleHistoryRetentionPolicy =
            DynamicScheduleHistoryRetentionPolicy(),
    ): DynamicScheduleHistoryCompactionBatchResult {

        val results =
            itemIds
                /*
                 * 去重：同一个 itemId 传两次不应该压缩两轮，
                 * 否则第二轮会基于"已经不完整"的历史再做决定。
                 */
                .distinct()
                .map { itemId ->
                    compactItem(
                        itemId = itemId,
                        now = now,
                        policy = policy,
                    )
                }

        return DynamicScheduleHistoryCompactionBatchResult(
            results = results,
        )
    }

    private suspend fun compactItem(
        itemId: DynamicScheduleItemId,

        now: Instant,

        policy: DynamicScheduleHistoryRetentionPolicy,
    ): DynamicScheduleHistoryCompactionResult {

        val snapshots =
            historyOf(itemId)
                .sortedWith(
                    compareByDescending<
                        DynamicScheduleDecisionSnapshot,
                        > {
                        it.capturedAt
                    }.thenByDescending {
                        it.id.value.toString()
                    },
                )

        if (snapshots.isEmpty()) {
            return emptyResult(itemId)
        }

        val latestId =
            snapshots.first().id

        val cutoff =
            policy.maxAge?.let {
                now - it
            }

        val deleteCandidates =
            snapshots.filterIndexed { index, snapshot ->

                val protectedLatest =
                    policy.preserveLatest &&
                        snapshot.id == latestId

                val exceedsCount =
                    policy.maxSnapshotsPerItem
                        ?.let { max ->
                            index >= max
                        }
                        ?: false

                val exceedsAge =
                    cutoff?.let {
                        snapshot.capturedAt < it
                    }
                        ?: false

                !protectedLatest &&
                    (exceedsCount || exceedsAge)
            }

        if (deleteCandidates.isEmpty()) {
            return emptyResult(itemId)
        }

        val traceIds =
            deleteCandidates
                .mapNotNull {
                    it.traceId
                }
                .toSet()

        var replayInputsDeleted = 0

        deleteCandidates.forEach { snapshot ->

            /*
             * ReplayInput 与 Snapshot 是一对一关系。
             *
             * 先删 ReplayInput，再删 Snapshot：
             * 中途失败时留下的是"有决策、缺重放输入"，
             * 而不是"有重放输入、但没有决策"。
             */
            if (replayInputStore.get(snapshot.id) != null) {

                replayInputStore.delete(snapshot.id)

                replayInputsDeleted++
            }

            snapshotStore.delete(snapshot.id)
        }

        /*
         * Snapshot 删除之后再判断 Trace：
         *
         * 只要还有任何一条 Snapshot 引用它，就不能删。
         */
        var tracesDeleted = 0

        traceIds.forEach { traceId ->

            if (
                !snapshotStillReferencesTrace(
                    itemId = itemId,
                    traceId = traceId,
                )
            ) {
                traceStore.delete(traceId)

                tracesDeleted++
            }
        }

        return DynamicScheduleHistoryCompactionResult(
            itemId = itemId,

            snapshotsDeleted =
                deleteCandidates.size,

            replayInputsDeleted =
                replayInputsDeleted,

            tracesDeleted =
                tracesDeleted,
        )
    }

    private suspend fun historyOf(
        itemId: DynamicScheduleItemId,
    ): List<DynamicScheduleDecisionSnapshot> =
        snapshotStore
            .query(
                DynamicScheduleDecisionSnapshotQuery(
                    itemId = itemId,

                    /*
                     * 当前 Query API 没有专门的"取全部"入口，
                     * 这里用一个足够大的 limit。
                     *
                     * 以后数据量大了再换成真正的分页 / 批量候选查询。
                     */
                    limit = Int.MAX_VALUE,
                ),
            )
            .snapshots

    private suspend fun snapshotStillReferencesTrace(
        itemId: DynamicScheduleItemId,

        traceId: DynamicScheduleDecisionTraceId,
    ): Boolean =
        historyOf(itemId).any {
            it.traceId == traceId
        }

    private fun emptyResult(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleHistoryCompactionResult =
        DynamicScheduleHistoryCompactionResult(
            itemId = itemId,
            snapshotsDeleted = 0,
            replayInputsDeleted = 0,
            tracesDeleted = 0,
        )
}
