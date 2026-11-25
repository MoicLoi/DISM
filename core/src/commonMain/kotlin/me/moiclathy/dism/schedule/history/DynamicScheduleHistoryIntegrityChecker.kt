package me.moiclathy.dism.schedule.history

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore

/**
 * 13.7
 *
 * 历史决策完整性检查。
 *
 * 一条完整 Decision 应该满足：
 *
 * ```text
 * Snapshot
 *  ├── itemId ─────────────┐
 *  ├── scheduleRevision ───┤ 必须一致
 *  ├── engineVersion ──────┤
 *  └── traceId ──► Trace ──┘
 *
 * Snapshot.id ──► ReplayInput
 *  ├── decisionSnapshot.id 必须回指同一条 Snapshot
 *  ├── scheduleRevision ── 必须一致
 *  ├── engineVersion ───── 必须一致
 *  └── decisionSnapshot.itemId 必须一致
 * ```
 *
 * ### 只读
 *
 * 13.7 刻意**只报告、不修复、不删除**。
 *
 * 因为发现
 *
 * ```text
 * Snapshot S1 的 traceId = T1，但 T1 不存在
 * ```
 *
 * 之后，系统其实无法知道应该：
 *
 * ```text
 * A. 删除 S1
 * B. 清空 S1.traceId
 * C. 找另一条 Trace
 * D. 重建 Trace
 * E. 保留 S1 并标记 degraded
 * ```
 *
 * 而 DISM 后面要做 Historical Replay / Decision Verification，
 * **绝不能偷偷改动历史证据**。
 *
 * 所以职责划分是：
 *
 * > Integrity Checker 负责发现问题，
 * > Repair Service（13.8）以后单独负责处理问题。
 */
class DynamicScheduleHistoryIntegrityChecker(
    private val snapshotStore:
        DynamicScheduleDecisionSnapshotStore,

    private val traceStore:
        DynamicScheduleDecisionTraceStore,

    private val replayInputStore:
        DynamicScheduleDecisionReplayInputSnapshotStore,

    /**
     * 13.17：双向引用检查需要 provenance。
     *
     * 为 null 时只做 13.7 的正向检查（向后兼容）。
     */
    private val provenanceStore:
        DynamicScheduleDecisionProvenanceStore? = null,
) {

    suspend fun check(
        itemId: DynamicScheduleItemId,
    ): DynamicScheduleHistoryIntegrityReport {

        val snapshots =
            historyOf(itemId)

        if (snapshots.isEmpty()) {

            /*
             * 没有历史不是"坏"。
             *
             * 首次启动、或者这个 item 从来没有产生过决策，
             * 都属于正常状态。
             */
            return DynamicScheduleHistoryIntegrityReport(
                itemId = itemId,

                status =
                    DynamicScheduleHistoryIntegrityStatus.VALID,

                checkedSnapshots = 0,
            )
        }

        val issues =
            buildList {

                snapshots.forEach { snapshot ->
                    checkSnapshot(
                        snapshot = snapshot,
                        issues = this,
                    )
                }

                /*
                 * 13.17：反方向也要查这一份。
                 *
                 * 否则 `ReplayInputMissingSnapshot` 只会出现在全库报告里，
                 * 而修复计划是 per-item 的——
                 * 13.9 的孤儿清理动作就永远不可达。
                 */
                addAll(orphanReplayInputsFor(itemId))

                /*
                 * Provenance → ReplayInput 的检查只在这里做一次。
                 *
                 * 不能让 checkSnapshot 也做一遍，否则全库检查时
                 * 同一条 issue 会被正向、反向各报一次。
                 */
                snapshots.forEach { snapshot ->
                    addAll(
                        provenanceIssuesFor(snapshot),
                    )
                }
            }

        return DynamicScheduleHistoryIntegrityReport(
            itemId = itemId,

            status = determineIntegrityStatus(issues),

            checkedSnapshots = snapshots.size,

            issues = issues,
        )
    }

    /**
     * 13.17：该 item 名下、父 Snapshot 已经不存在的 ReplayInput。
     */
    private suspend fun orphanReplayInputsFor(
        itemId: DynamicScheduleItemId,
    ): List<DynamicScheduleHistoryIntegrityIssue> =
        replayInputStore
            .allSnapshotIds()
            .mapNotNull { snapshotId ->

                if (snapshotStore.get(snapshotId) != null) {
                    return@mapNotNull null
                }

                val orphan =
                    replayInputStore.get(snapshotId)
                        ?: return@mapNotNull null

                if (orphan.decisionSnapshot.itemId != itemId) {
                    return@mapNotNull null
                }

                DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputMissingSnapshot(
                        itemId = itemId,
                        snapshotId = snapshotId,
                    )
            }

    /**
     * 13.17：全库**双向**引用完整性检查。
     *
     * 与 [check] 的区别不只是"范围更大"，而是**方向也更多**：
     *
     * ```text
     * Snapshot    → 它引用的 Trace / ReplayInput / Provenance 存在吗？
     * ReplayInput → 它的 Snapshot 还在吗？
     * Provenance  → 它的 Snapshot / ReplayInput 还在吗？
     * ```
     *
     * 反向检查是必需的：Snapshot 被压缩掉之后，
     * 那些孤儿记录再也无法通过"遍历 Snapshot"发现。
     *
     * ### 两条刻意的规则
     *
     * 1. **Snapshot → ReplayInput 是"可选"**（13.17.16）。
     *    Retention 会连带删除旧 ReplayInput，这是正常状态，
     *    不能把它当成完整性错误。
     * 2. **Provenance → ReplayInput 是"必须"**。
     *    Provenance 明确表示"这个 Snapshot 曾经被重放过"，
     *    那么它当时用的输入就应该还在。
     *
     * 它只读、只报告，**不做任何修复**（13.17.19）——
     * 一旦检测算法本身有 bug，自动修复会把历史二次破坏。
     */
    suspend fun checkAll(
        checkedAt: kotlin.time.Instant,
    ): DynamicScheduleHistoryIntegrityReport {

        val snapshots =
            snapshotStore
                .query(
                    DynamicScheduleDecisionSnapshotQuery(
                        itemId = null,
                        limit = Int.MAX_VALUE,
                    ),
                )
                .snapshots

        val issues =
            mutableListOf<
                DynamicScheduleHistoryIntegrityIssue
                >()

        snapshots.forEach { snapshot ->
            checkSnapshot(
                snapshot = snapshot,
                issues = issues,
            )
        }

        /*
         * 反方向：ReplayInput → Snapshot。
         *
         * 13.9 为孤儿检测加的枚举能力正好用在这里。
         */
        val replayInputSnapshotIds =
            replayInputStore.allSnapshotIds()

        replayInputSnapshotIds.forEach { snapshotId ->

            if (snapshotStore.get(snapshotId) != null) {
                return@forEach
            }

            /*
             * 孤儿 ReplayInput 没有父 Snapshot，
             * 只能从它自己的 decisionSnapshot 里取 itemId。
             */
            val orphan =
                replayInputStore.get(snapshotId)
                    ?: return@forEach

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputMissingSnapshot(
                        itemId =
                            orphan.decisionSnapshot.itemId,

                        snapshotId = snapshotId,
                    )
        }

        /*
         * 反方向：Provenance → Snapshot / ReplayInput。
         *
         * 13.17.13：必须遍历**全部** provenance，
         * 只看 latest 会漏掉更早的坏记录。
         */
        val provenances =
            provenanceStore?.all().orEmpty()

        provenances.forEach { provenance ->

            if (
                snapshotStore.get(provenance.snapshotId) == null
            ) {
                issues +=
                    DynamicScheduleHistoryIntegrityIssue
                        .ProvenanceMissingSnapshot(
                            itemId = provenance.itemId,

                            provenanceId = provenance.id,

                            snapshotId =
                                provenance.snapshotId,
                        )
            }

            if (
                replayInputStore.get(
                    provenance.snapshotId,
                ) == null
            ) {
                issues +=
                    DynamicScheduleHistoryIntegrityIssue
                        .ProvenanceMissingReplayInput(
                            itemId = provenance.itemId,

                            provenanceId = provenance.id,

                            snapshotId =
                                provenance.snapshotId,
                        )
            }

            /*
             * 13.17.10：verification 声称验证过一次重放，
             * 但重放本身的信息不完整。
             *
             * verification.replayEngineVersion 由 data class
             * 保证非空，所以这里检查的是"整条重放事实是否齐备"。
             */
            provenance.verification?.let {
                if (
                    provenance.replayEngineVersion.isNullOrBlank() ||
                    provenance.behaviorFingerprint.isNullOrBlank()
                ) {
                    issues +=
                        DynamicScheduleHistoryIntegrityIssue
                            .VerificationWithoutReplay(
                                itemId = provenance.itemId,
                                provenanceId = provenance.id,
                            )
                }
            }

            /*
             * 注意这里**不**产生 VerificationEngineMismatch。
             *
             * Replay 用 13.15、Verify 用 13.16 是允许的
             * （13.17.9），把版本差异当成历史损坏会误报。
             */
        }

        return DynamicScheduleHistoryIntegrityReport(
            itemId = null,

            status = determineIntegrityStatus(issues),

            checkedSnapshots = snapshots.size,

            checkedReplayInputs =
                replayInputSnapshotIds.size,

            checkedProvenances = provenances.size,

            issues = issues,

            checkedAt = checkedAt,
        )
    }

    suspend fun check(
        itemIds: Collection<DynamicScheduleItemId>,
    ): DynamicScheduleHistoryIntegrityBatchReport {

        val reports =
            itemIds
                .distinct()
                .map { itemId ->
                    check(itemId)
                }

        return DynamicScheduleHistoryIntegrityBatchReport(
            reports = reports,
        )
    }

    private suspend fun checkSnapshot(
        snapshot: DynamicScheduleDecisionSnapshot,

        issues:
            MutableList<DynamicScheduleHistoryIntegrityIssue>,
    ) {

        checkTraceReference(
            snapshot = snapshot,
            issues = issues,
        )

        checkReplayInputReference(
            snapshot = snapshot,
            issues = issues,
        )
    }

    /**
     * 13.17：Snapshot → Provenance（该 Snapshot 的**全部**重放记录）。
     *
     * 只查 latest 会漏掉"更早的一次重放坏了"这种情况。
     */
    private suspend fun provenanceIssuesFor(
        snapshot: DynamicScheduleDecisionSnapshot,
    ): List<DynamicScheduleHistoryIntegrityIssue> {

        val store = provenanceStore
            ?: return emptyList()

        return store
            .queryForSnapshot(snapshot.id)
            .mapNotNull { provenance ->

                if (
                    replayInputStore.get(snapshot.id) != null
                ) {
                    return@mapNotNull null
                }

                DynamicScheduleHistoryIntegrityIssue
                    .ProvenanceMissingReplayInput(
                        itemId = snapshot.itemId,

                        provenanceId = provenance.id,

                        snapshotId = snapshot.id,
                    )
            }
    }

    /**
     * Snapshot → Trace。
     */
    private suspend fun checkTraceReference(
        snapshot: DynamicScheduleDecisionSnapshot,

        issues:
            MutableList<DynamicScheduleHistoryIntegrityIssue>,
    ) {

        val traceId =
            snapshot.traceId

        if (traceId == null) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .SnapshotWithoutTrace(
                        itemId = snapshot.itemId,
                        snapshotId = snapshot.id,
                    )

            return
        }

        val trace =
            traceStore.get(traceId)

        if (trace == null) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .MissingTrace(
                        itemId = snapshot.itemId,
                        snapshotId = snapshot.id,
                        traceId = traceId,
                    )

            return
        }

        if (trace.itemId != snapshot.itemId) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .TraceItemMismatch(
                        itemId = snapshot.itemId,

                        snapshotId = snapshot.id,

                        traceId = traceId,

                        snapshotItemId =
                            snapshot.itemId.value,

                        traceItemId =
                            trace.itemId.value,
                    )
        }

        if (
            trace.scheduleRevision !=
            snapshot.scheduleRevision
        ) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .ScheduleRevisionMismatch(
                        itemId = snapshot.itemId,

                        snapshotId = snapshot.id,

                        traceId = traceId,

                        snapshotRevision =
                            snapshot.scheduleRevision,

                        traceRevision =
                            trace.scheduleRevision,
                    )
        }

        if (
            trace.engineVersion !=
            snapshot.engineVersion
        ) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .EngineVersionMismatch(
                        itemId = snapshot.itemId,

                        snapshotId = snapshot.id,

                        traceId = traceId,

                        snapshotEngineVersion =
                            snapshot.engineVersion,

                        traceEngineVersion =
                            trace.engineVersion,
                    )
        }
    }

    /**
     * Snapshot → ReplayInput。
     */
    private suspend fun checkReplayInputReference(
        snapshot: DynamicScheduleDecisionSnapshot,

        issues:
            MutableList<DynamicScheduleHistoryIntegrityIssue>,
    ) {

        val replayInput =
            replayInputStore.get(snapshot.id)

        if (replayInput == null) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .MissingReplayInput(
                        itemId = snapshot.itemId,
                        snapshotId = snapshot.id,
                    )

            return
        }

        /*
         * ReplayInput 必须回指同一条 Snapshot。
         */
        if (
            replayInput.decisionSnapshot.id !=
            snapshot.id
        ) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputSnapshotMismatch(
                        itemId = snapshot.itemId,

                        snapshotId = snapshot.id,

                        replaySnapshotId =
                            replayInput
                                .decisionSnapshot
                                .id,
                    )
        }

        if (
            replayInput.decisionSnapshot.itemId !=
            snapshot.itemId
        ) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputItemMismatch(
                        itemId = snapshot.itemId,

                        snapshotId = snapshot.id,

                        replayItemId =
                            replayInput
                                .decisionSnapshot
                                .itemId
                                .value,
                    )
        }

        if (
            replayInput.scheduleRevision !=
            snapshot.scheduleRevision
        ) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputRevisionMismatch(
                        itemId = snapshot.itemId,

                        snapshotId = snapshot.id,

                        snapshotRevision =
                            snapshot.scheduleRevision,

                        replayRevision =
                            replayInput.scheduleRevision,
                    )
        }

        if (
            replayInput.engineVersion !=
            snapshot.engineVersion
        ) {

            issues +=
                DynamicScheduleHistoryIntegrityIssue
                    .ReplayInputEngineVersionMismatch(
                        itemId = snapshot.itemId,

                        snapshotId = snapshot.id,

                        snapshotEngineVersion =
                            snapshot.engineVersion,

                        replayEngineVersion =
                            replayInput.engineVersion,
                    )
        }

        /*
         * 刻意**不**检查 replayInput.input.model.items 里有没有这个 item。
         *
         * 因为 DynamicScheduleIntelligenceInput.model 是完整 Schedule Model：
         *
         * ```text
         * Schedule revision R10
         *  ├─ item A
         *  ├─ item B
         *  └─ item C
         * ```
         *
         * 而 Snapshot 只是针对 A 的一次 Decision。
         * 把 model.items 误读成"只能有 A"会制造假警报。
         *
         * 可靠的关联只有 decisionSnapshot（上面已经检查过）。
         */
    }



    private suspend fun historyOf(
        itemId: DynamicScheduleItemId,
    ): List<DynamicScheduleDecisionSnapshot> =
        snapshotStore
            .query(
                DynamicScheduleDecisionSnapshotQuery(
                    itemId = itemId,
                    limit = Int.MAX_VALUE,
                ),
            )
            .snapshots
}
