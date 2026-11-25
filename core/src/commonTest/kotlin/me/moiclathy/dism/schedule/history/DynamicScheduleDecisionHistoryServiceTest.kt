package me.moiclathy.dism.schedule.history

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceBuilder
import me.moiclathy.dism.schedule.provenance.DynamicScheduleReplayVerificationProvenance
import me.moiclathy.dism.schedule.provenance.InMemoryDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.replay.DeterministicDynamicScheduleHistoricalRuntimeReplayEngine
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotPersistenceService
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayService
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationService
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationStatus
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibility

/**
 * 13.13.13
 *
 * 统一历史查询。
 *
 * 最关键的是 [defaultQueryNeverReplays]：
 *
 * > **查询历史 ≠ 重新执行历史。**
 */
class DynamicScheduleDecisionHistoryServiceTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val now: Instant =
        Instant.parse("2026-09-24T08:00:00Z")

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val provenanceStore =
        InMemoryDynamicScheduleDecisionProvenanceStore()

    private val provenanceBuilder =
        DynamicScheduleDecisionProvenanceBuilder()

    private val snapshotPersistence =
        DynamicScheduleDecisionSnapshotPersistenceService(
            snapshots,
        )

    private val tracePersistence =
        DynamicScheduleDecisionTracePersistenceService(
            traces,
        )

    /**
     * 默认查询：**不**注入 replay / verification。
     *
     * 这样 [defaultQueryNeverReplays] 才能证明
     * "查询历史不会碰 Runtime"——
     * 就算依赖是 null，默认路径也必须正常工作。
     */
    private val readOnlyService =
        DeterministicDynamicScheduleDecisionHistoryService(
            snapshotPersistence = snapshotPersistence,
            tracePersistence = tracePersistence,
            provenanceStore = provenanceStore,
        )

    // -----------------------------------------------------------------
    // ① 没有历史
    // -----------------------------------------------------------------

    @Test
    fun emptyHistoryIsAnEmptyResult() = runTest {

        val result = readOnlyService.query(query())

        assertTrue(result.isEmpty)

        assertEquals(0, result.totalMatched)

        assertNull(result.latest)
    }

    // -----------------------------------------------------------------
    // ② Snapshot + Trace（通过 traceId，而不是 latest）
    // -----------------------------------------------------------------

    @Test
    fun traceIsFoundThroughSnapshotTraceId() = runTest {

        val snapshot = saveSnapshot(hoursAgo = 2)

        val trace = saveTrace(snapshot.traceId!!)

        /*
         * 故意再放一条同 item 的"更新"Trace：
         * 按 traceId 精确查找必须仍然拿到 Snapshot 指向的那条。
         */
        saveTrace(DynamicScheduleDecisionTraceId())

        val entry =
            assertNotNull(
                readOnlyService.query(query()).latest,
            )

        assertEquals(trace, entry.trace)

        assertTrue(entry.hasTrace)

        assertEquals(snapshot.id, entry.snapshotId)

        assertEquals(snapshot.capturedAt, entry.capturedAt)
    }

    @Test
    fun includeTraceFalseSkipsTraceLoading() = runTest {

        val snapshot = saveSnapshot(hoursAgo = 1)

        saveTrace(snapshot.traceId!!)

        val entry =
            assertNotNull(
                readOnlyService
                    .query(
                        query().copy(includeTrace = false),
                    )
                    .latest,
            )

        assertFalse(entry.hasTrace)
    }

    @Test
    fun traceStageFilterStillLoadsTrace() = runTest {

        val snapshot = saveSnapshot(hoursAgo = 1)

        saveTrace(snapshot.traceId!!)

        /*
         * includeTrace=false 但带了 stage 过滤：
         * needsTrace 必须把 Trace 拉回来，
         * 否则过滤条件形同虚设。
         */
        val result =
            readOnlyService.query(
                query().copy(
                    includeTrace = false,
                    traceStage =
                        DynamicScheduleDecisionTraceStage.OUTPUT,
                ),
            )

        assertEquals(1, result.size)
    }

    // -----------------------------------------------------------------
    // ③ 默认不 Replay
    // -----------------------------------------------------------------

    @Test
    fun defaultQueryNeverReplays() = runTest {

        /*
         * 13.13 的核心语义：
         *
         * 一次普通历史查询**不能**变成
         * "把 N 条历史重新执行 N 次"。
         *
         * readOnlyService 里根本没有 replayService，
         * 默认查询依然必须完整返回 Snapshot。
         */
        repeat(3) { index ->
            saveSnapshot(hoursAgo = index + 1)
        }

        val result = readOnlyService.query(query())

        assertEquals(3, result.size)

        result.entries.forEach { entry ->
            assertNull(entry.replay, "默认查询不允许重放")

            assertNull(entry.verification)

            assertFalse(entry.hasReplay)
        }
    }

    // -----------------------------------------------------------------
    // ④ Provenance 是读出来的，不是跑出来的
    // -----------------------------------------------------------------

    @Test
    fun provenanceIsLoadedWithoutReplaying() = runTest {

        val snapshot = saveSnapshot(hoursAgo = 1)

        val provenance =
            provenanceBuilder.build(
                snapshot = snapshot,
                historicalEngineVersion = "12.27",
                replayEngineVersion = "13.11",
                compatibility =
                    DynamicScheduleHistoryVersionCompatibility
                        .EXACT,
                migrationId = null,
                evaluatedAt = now,
                replayedAt = now,
                verification =
                    DynamicScheduleReplayVerificationProvenance(
                        verifiedAt = now,
                        status =
                            DynamicScheduleReplayVerificationStatus
                                .EXACT_MATCH,
                        historicalEngineVersion = "12.27",
                        replayEngineVersion = "13.11",
                        differenceCount = 0,
                    ),
            )

        provenanceStore.save(provenance)

        val entry =
            assertNotNull(
                readOnlyService.query(query()).latest,
            )

        assertEquals(provenance.id, entry.provenance?.id)

        assertTrue(entry.wasReplayed)

        /*
         * 关键：默认查询不重放，但仍然能回答
         * "上次验证是什么结论"。
         */
        assertEquals(
            "EXACT_MATCH",
            entry.knownVerificationStatus,
        )

        assertFalse(entry.hasReplay)

        assertEquals(now, entry.lastReplayedAt)
    }

    @Test
    fun includeProvenanceFalseSkipsTheLookup() = runTest {

        val snapshot = saveSnapshot(hoursAgo = 1)

        provenanceStore.save(
            provenanceBuilder.build(
                snapshot = snapshot,
                historicalEngineVersion = "12.27",
                replayEngineVersion = "13.11",
                compatibility =
                    DynamicScheduleHistoryVersionCompatibility
                        .EXACT,
                migrationId = null,
                evaluatedAt = now,
                replayedAt = now,
            ),
        )

        val entry =
            assertNotNull(
                readOnlyService
                    .query(
                        query().copy(
                            includeProvenance = false,
                        ),
                    )
                    .latest,
            )

        assertFalse(entry.wasReplayed)

        assertNull(entry.knownVerificationStatus)
    }

    // -----------------------------------------------------------------
    // ⑤ 非法配置
    // -----------------------------------------------------------------

    @Test
    fun verificationWithoutReplayIsRejected() {

        assertFailsWith<IllegalArgumentException> {
            DynamicScheduleDecisionHistoryQuery(
                includeReplay = false,
                includeVerification = true,
            )
        }
    }

    @Test
    fun invalidWindowIsRejected() {

        assertFailsWith<IllegalArgumentException> {
            DynamicScheduleDecisionHistoryQuery(
                from = now,
                until = now - 1.hours,
            )
        }
    }

    // -----------------------------------------------------------------
    // 排序与分页
    // -----------------------------------------------------------------

    @Test
    fun entriesAreNewestFirst() = runTest {

        val oldest = saveSnapshot(hoursAgo = 5)

        val middle = saveSnapshot(hoursAgo = 3)

        val newest = saveSnapshot(hoursAgo = 1)

        val result = readOnlyService.query(query())

        assertEquals(
            listOf(newest.id, middle.id, oldest.id),
            result.entries.map { it.snapshotId },
        )

        assertEquals(newest.id, result.latest?.snapshotId)
    }

    @Test
    fun limitTruncatesButReportsTotalMatched() = runTest {

        repeat(5) { index ->
            saveSnapshot(hoursAgo = index + 1)
        }

        val result =
            readOnlyService.query(query().copy(limit = 2))

        assertEquals(2, result.size)

        assertEquals(5, result.totalMatched)

        assertTrue(result.truncated)
    }

    @Test
    fun otherItemsAreNotMixedIn() = runTest {

        saveSnapshot(hoursAgo = 1)

        saveSnapshot(
            hoursAgo = 1,
            itemId = DynamicScheduleItemId("other-item"),
        )

        val result = readOnlyService.query(query())

        assertEquals(1, result.size)

        assertEquals(itemId, result.latest?.itemId)
    }

    // -----------------------------------------------------------------
    // ⑥ 真实重放：每条历史只重放一次
    // -----------------------------------------------------------------

    @Test
    fun includeReplayAndVerificationReplaysEachEntryOnlyOnce() =
        runTest {

            /*
             * 这条测试锁的是对设计稿的一处修正。
             *
             * 设计稿在 includeVerification 时同时调用
             * `replayService.replay(id)` 与 `verificationService.verify(id)`，
             * 而后者内部又会重放一次——
             * 同一条历史被重放两次，并留下**两条** Provenance。
             *
             * 这里用真实的 Replay / Verification 服务，
             * 通过"来源链条数"来观察重放次数。
             */
            val snapshot = saveSnapshot(hoursAgo = 1)

            saveTrace(snapshot.traceId!!)

            /*
             * ReplayInput 与 Snapshot 共用同一个 id，
             * 否则 replay 找不到输入。
             */
            replayInputs.save(
                DynamicScheduleReplayTestData
                    .create()
                    .copy(decisionSnapshot = snapshot),
            )

            val replayService =
                DynamicScheduleHistoricalRuntimeReplayService(
                    replayInputPersistence =
                        DynamicScheduleDecisionReplayInputSnapshotPersistenceService(
                            replayInputs,
                        ),

                    snapshotPersistence = snapshotPersistence,

                    tracePersistence = tracePersistence,

                    replayEngine =
                        DeterministicDynamicScheduleHistoricalRuntimeReplayEngine(
                            runtime =
                                DynamicScheduleReplayTestData
                                    .runtime(),
                        ),

                    replayEngineVersion =
                        DynamicScheduleReplayTestData
                            .ENGINE_VERSION,

                    provenanceStore = provenanceStore,
                )

            val verificationService =
                DynamicScheduleReplayVerificationService(
                    replayService = replayService,

                    replayEngineVersion =
                        DynamicScheduleReplayTestData
                            .ENGINE_VERSION,

                    provenanceStore = provenanceStore,
                )

            val service =
                DeterministicDynamicScheduleDecisionHistoryService(
                    snapshotPersistence = snapshotPersistence,

                    tracePersistence = tracePersistence,

                    provenanceStore = provenanceStore,

                    replayService = replayService,

                    verificationService = verificationService,
                )

            val entry =
                assertNotNull(
                    service
                        .query(
                            query().copy(
                                includeReplay = true,
                                includeVerification = true,
                            ),
                        )
                        .latest,
                )

            assertNotNull(entry.replay)

            assertNotNull(entry.verification)

            assertEquals(
                "EXACT_MATCH",
                entry.knownVerificationStatus,
            )

            /*
             * 一个决策一次重放 → 恰好一条来源链。
             * 如果是两条，说明发生了重复重放。
             */
            assertEquals(
                1,
                provenanceStore.queryForItem(itemId).size,
                "includeVerification 不应该导致第二次重放",
            )

            assertNotNull(entry.provenance)

            assertEquals(
                snapshot.id,
                entry.provenance?.snapshotId,
            )
        }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private fun query() =
        DynamicScheduleDecisionHistoryQuery(
            itemId = itemId,
        )

    private suspend fun saveSnapshot(
        hoursAgo: Int,

        itemId: DynamicScheduleItemId = this.itemId,
    ): DynamicScheduleDecisionSnapshot {

        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(
                    id = DynamicScheduleDecisionSnapshotId(),
                    itemId = itemId,
                    capturedAt = now - hoursAgo.hours,
                )

        snapshots.save(snapshot)

        return snapshot
    }

    private suspend fun saveTrace(
        traceId: DynamicScheduleDecisionTraceId,
    ): DynamicScheduleDecisionTraceRecord {

        val record =
            DynamicScheduleDecisionTraceRecord(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(itemId)
                        .copy(id = traceId)
                        .withEntry(
                            stage =
                                DynamicScheduleDecisionTraceStage
                                    .OUTPUT,

                            message = "Output",

                            decisive = true,
                        )
                        /*
                         * 必须包含 EXPLANATION 阶段。
                         *
                         * 否则 12.28 的 explanation parity 会认为
                         * "历史无解释 / 重放有解释"，
                         * 把结论判成 BEHAVIOR_CHANGED——
                         * 那是 Trace 夹具不完整，不是系统出错。
                         */
                        .withEntry(
                            stage =
                                DynamicScheduleDecisionTraceStage
                                    .EXPLANATION,

                            message =
                                "Historical decision produced an explanation.",
                        ),

                createdAt = now,

                scheduleRevision = 0,
            )

        traces.save(record)

        return record
    }
}
