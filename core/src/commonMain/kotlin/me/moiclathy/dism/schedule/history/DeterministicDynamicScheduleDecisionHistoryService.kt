package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayService
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationReport
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationService
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService

/**
 * 12.x / 13.13
 *
 * 把 Snapshot / Trace / Provenance / Replay / Verification
 * 拼装成统一的 Decision History。
 *
 * 这一层存在的意义是让 App 不再需要分别调用：
 *
 * ```text
 * snapshotFor()
 * traceFor()
 * provenanceFor()
 * replay()
 * verifyReplay()
 * ```
 *
 * ### 成本边界（13.13.5）
 *
 * `includeReplay = true` 时是明确的昂贵操作：
 *
 * ```text
 * N 个 Snapshot → N 次 Replay（+ N 次 Verification）
 * ```
 *
 * 所以它不是默认路径。批量重放的并发控制属于 13.14，
 * 这里不提前把两件事混在一起。
 *
 * ### 一处对设计稿的修正
 *
 * 设计稿在 `includeVerification` 时同时调用了
 * `replayService.replay(id)` 和 `verificationService.verify(id)`，
 * 而后者内部又会重放一次——同一条历史会被重放两次，
 * 并且留下两条 Provenance。
 *
 * 这里改成：重放**一次**，然后用
 * `verificationService.verify(replay)` 对同一个结果做校验。
 */
class DeterministicDynamicScheduleDecisionHistoryService(
    private val snapshotPersistence:
        DynamicScheduleDecisionSnapshotPersistenceService,

    private val tracePersistence:
        DynamicScheduleDecisionTracePersistenceService,

    /**
     * 13.12：已有的来源链（读取，不重放）。
     */
    private val provenanceStore:
        DynamicScheduleDecisionProvenanceStore? = null,

    /**
     * 13.13：只有 [DynamicScheduleDecisionHistoryQuery.includeReplay]
     * 为 true 时才会用到。
     */
    private val replayService:
        DynamicScheduleHistoricalRuntimeReplayService? = null,

    private val verificationService:
        DynamicScheduleReplayVerificationService? = null,
) : DynamicScheduleDecisionHistoryService {

    override suspend fun query(
        query: DynamicScheduleDecisionHistoryQuery,
    ): DynamicScheduleDecisionHistoryResult {

        val snapshotResult =
            snapshotPersistence.query(
                query.toSnapshotQuery(),
            )

        val entries =
            snapshotResult.snapshots
                .map { snapshot ->
                    buildEntry(snapshot, query)
                }
                .filter { entry ->
                    matchesTraceStage(entry, query)
                }
                .filter { entry ->
                    matchesDecisiveOnly(entry, query)
                }
                .take(query.limit)

        return DynamicScheduleDecisionHistoryResult(
            entries = entries,

            /*
             * 13.13：truncated 的语义修正。
             *
             * 原来的判据是
             * `snapshotResult.snapshots.size > entries.size`，
             * 它只有在**后置过滤**（traceStage / decisiveOnly）
             * 丢掉条目时才为真；
             * 而 limit 造成的截断在 store 层就已经发生，
             * 快照列表本身就只有 limit 条，
             * 于是"还有更多历史"这件事被漏报了。
             *
             * 有了 totalMatched（store 返回的**未截断**匹配数）
             * 之后，正确判据是它大于实际返回条数——
             * 既覆盖 limit 截断，也覆盖后置过滤丢弃。
             */
            truncated =
                snapshotResult.totalMatched > entries.size,

            itemId = query.itemId,

            totalMatched = snapshotResult.totalMatched,
        )
    }

    private suspend fun buildEntry(
        snapshot:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshot,

        query: DynamicScheduleDecisionHistoryQuery,
    ): DynamicScheduleDecisionHistoryEntry {

        /*
         * Trace 通过 snapshot.traceId 精确查找，
         * 而不是"该 item 最新 Trace"——
         * 这是 13.4 建立的一致性规则。
         */
        val trace =
            if (query.needsTrace) {
                snapshot.traceId?.let {
                    tracePersistence.find(it)
                }
            } else {
                null
            }

        /*
         * 顺序很重要：**先重放，再读来源链**。
         *
         * 如果反过来，本次查询刚刚创建的那条 Provenance
         * 就不会出现在结果里（读到的是重放之前的快照视图），
         * 于是"我刚重放过，但 entry 说没重放过"。
         */
        var replay:
            DynamicScheduleHistoricalRuntimeReplayResult? = null

        var verification:
            DynamicScheduleReplayVerificationReport? = null

        if (query.includeReplay) {

            /*
             * 重放一次，然后（如果需要）对**同一个结果**做校验。
             * 这样 includeVerification 不会导致第二次重放。
             */
            val replayed =
                replayService?.replay(snapshot.id)

            replay = replayed

            if (
                replayed != null &&
                query.includeVerification
            ) {
                verification =
                    verificationService?.verify(replayed)
            }
        }

        val provenance =
            if (query.includeProvenance) {
                provenanceStore?.latestForSnapshot(snapshot.id)
            } else {
                null
            }

        return DynamicScheduleDecisionHistoryEntry(
            snapshot = snapshot,
            trace = trace,
            provenance = provenance,
            replay = replay,
            verification = verification,
        )
    }

    private fun matchesTraceStage(
        entry: DynamicScheduleDecisionHistoryEntry,
        query: DynamicScheduleDecisionHistoryQuery,
    ): Boolean {

        val stage = query.traceStage ?: return true

        return entry.trace?.trace?.entries?.any {
            it.stage == stage
        } == true
    }

    private fun matchesDecisiveOnly(
        entry: DynamicScheduleDecisionHistoryEntry,
        query: DynamicScheduleDecisionHistoryQuery,
    ): Boolean {

        if (!query.decisiveOnly) {
            return true
        }

        return entry.trace
            ?.trace
            ?.decisiveEntries
            ?.isNotEmpty() == true
    }
}
