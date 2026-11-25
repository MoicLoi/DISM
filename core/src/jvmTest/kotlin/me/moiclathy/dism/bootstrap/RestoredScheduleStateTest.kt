package me.moiclathy.dism.bootstrap

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.persistence.DismPersistence
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQuery
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotQueryResult
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQuery
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceQueryResult
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStore

/**
 * 13.4.5
 *
 * 恢复一致性测试。
 *
 * 重点不是"构造一个假的 Snapshot"，
 * 而是验证恢复对象的关联规则：
 *
 * ```text
 * traceFor(item)
 *       │
 *       ▼
 * snapshotFor(item)
 *       │
 *       ▼ snapshot.traceId
 *     exact trace
 * ```
 *
 * fixture 直接复用项目现有的
 * [DynamicScheduleReplayTestData]，
 * 不再复制一套"最小假数据"。
 */
class RestoredScheduleStateTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val otherItemId =
        DynamicScheduleItemId("item-2")

    @Test
    fun traceForUsesSnapshotTraceId() {

        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()

        val trace =
            traceRecord(
                traceId = requireNotNull(snapshot.traceId),
            )

        val state =
            RestoredScheduleState(
                snapshots = listOf(snapshot),

                traces =
                    listOf(
                        /*
                         * 故意再放一条同 item 的"更新"Trace：
                         * 按照旧语义（取最新）会拿到它，
                         * 按照 13.4 语义必须仍然拿到
                         * Snapshot 指向的那一条。
                         */
                        traceRecord(
                            traceId =
                                DynamicScheduleDecisionTraceId(),
                        ),

                        trace,
                    ),
            )

        assertEquals(
            trace,
            state.traceFor(itemId),
        )

        assertEquals(
            snapshot.traceId,
            state.traceFor(itemId)?.traceId,
        )
    }

    @Test
    fun unrelatedTraceIsNotReturned() {

        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()

        val unrelatedTrace =
            traceRecord(
                traceId =
                    DynamicScheduleDecisionTraceId(),
            )

        val state =
            RestoredScheduleState(
                snapshots = listOf(snapshot),

                traces = listOf(unrelatedTrace),
            )

        assertNull(
            state.traceFor(itemId),
            "无关 Trace 不能被当成该 item 的历史解释链",
        )
    }

    @Test
    fun diagnosticsAreScopedToItemId() {

        val state =
            RestoredScheduleState(
                diagnostics =
                    listOf(
                        ScheduleRestoreDiagnostic
                            .SnapshotNotFound(itemId = itemId),

                        ScheduleRestoreDiagnostic
                            .SnapshotNotFound(
                                itemId = otherItemId,
                            ),
                    ),
            )

        assertTrue(state.hasDiagnostics)

        assertEquals(
            1,
            state.diagnosticsFor(itemId).size,
        )

        assertEquals(
            otherItemId,
            (
                state.diagnosticsFor(otherItemId)
                    .single()
                    as ScheduleRestoreDiagnostic
                    .SnapshotNotFound
                ).itemId,
        )
    }

    @Test
    fun emptyStateIsEmptyAndWithoutDiagnostics() {

        val state = RestoredScheduleState()

        assertTrue(state.isEmpty)

        assertFalse(state.hasDiagnostics)

        assertNull(state.snapshotFor(itemId))

        assertNull(state.traceFor(itemId))
    }

    // -----------------------------------------------------------------
    // 真实恢复路径（含诊断）
    // -----------------------------------------------------------------

    @Test
    fun restoreReportsSnapshotNotFoundForUnknownItem() =
        runTest {

            val restored =
                CoreBootstrapper(DismPersistence())
                    .restoreSchedule(listOf(itemId))

            assertTrue(restored.isEmpty)

            assertTrue(
                restored
                    .diagnosticsFor(itemId)
                    .single()
                    is ScheduleRestoreDiagnostic
                    .SnapshotNotFound,
            )
        }

    @Test
    fun restoreReportsTraceMissingWhenTraceWasLost() =
        runTest {

            val persistence = DismPersistence()

            val snapshot =
                DynamicScheduleReplayTestData
                    .decisionSnapshot()

            /*
             * 只写 Snapshot，不写 Trace：
             * 模拟 "Snapshot 成功 / Trace 失败"。
             */
            persistence.scheduleSnapshots.save(snapshot)

            val restored =
                CoreBootstrapper(persistence)
                    .restoreSchedule(listOf(itemId))

            assertEquals(1, restored.snapshots.size)

            assertTrue(restored.traces.isEmpty())

            val diagnostic =
                restored.diagnosticsFor(itemId).single()

            assertTrue(
                diagnostic is ScheduleRestoreDiagnostic
                    .TraceMissing,
            )

            assertEquals(
                snapshot.traceId,
                (
                    diagnostic as
                        ScheduleRestoreDiagnostic.TraceMissing
                    ).traceId,
            )
        }

    @Test
    fun restoreReportsSnapshotWithoutTrace() =
        runTest {

            val persistence = DismPersistence()

            val snapshot =
                DynamicScheduleReplayTestData
                    .decisionSnapshot()
                    .copy(traceId = null)

            persistence.scheduleSnapshots.save(snapshot)

            val restored =
                CoreBootstrapper(persistence)
                    .restoreSchedule(listOf(itemId))

            assertTrue(
                restored
                    .diagnosticsFor(itemId)
                    .single()
                    is ScheduleRestoreDiagnostic
                    .SnapshotWithoutTrace,
            )
        }

    @Test
    fun restoreReportsTraceUnreadableWhenTraceIsCorrupted() =
        runTest {

            val snapshot =
                DynamicScheduleReplayTestData
                    .decisionSnapshot()

            /*
             * Trace Store 换成读取总是失败、写入正常的实现：
             * Snapshot 能读出来，Trace 读不出来。
             */
            val persistence =
                DismPersistence(
                    scheduleTraces = ExplodingTraceStore(),
                )

            persistence.scheduleSnapshots.save(snapshot)

            val restored =
                CoreBootstrapper(persistence)
                    .restoreSchedule(listOf(itemId))

            assertEquals(1, restored.snapshots.size)

            assertTrue(restored.traces.isEmpty())

            assertTrue(
                restored
                    .diagnosticsFor(itemId)
                    .single()
                    is ScheduleRestoreDiagnostic
                    .TraceUnreadable,
            )

            /*
             * 关键：整个恢复过程没有抛异常，
             * App 仍然能拿到 Snapshot 并启动。
             */
            assertEquals(
                snapshot.id,
                restored.snapshotFor(itemId)?.id,
            )
        }

    @Test
    fun restoreSurvivesUnreadableSnapshot() =
        runTest {

            val persistence =
                DismPersistence(
                    scheduleSnapshots =
                        ExplodingSnapshotStore(),
                )

            /*
             * 13.4.6：历史读不出来不能阻止启动。
             */
            val restored =
                CoreBootstrapper(persistence)
                    .restoreSchedule(listOf(itemId))

            assertTrue(restored.isEmpty)

            assertTrue(
                restored
                    .diagnosticsFor(itemId)
                    .single()
                    is ScheduleRestoreDiagnostic
                    .SnapshotUnreadable,
            )
        }

    private fun traceRecord(
        traceId: DynamicScheduleDecisionTraceId,
    ): DynamicScheduleDecisionTraceRecord =
        DynamicScheduleDecisionTraceRecord(
            trace =
                DeterministicDynamicScheduleDecisionTraceBuilder()
                    .create(itemId)
                    .copy(id = traceId),

            createdAt =
                DynamicScheduleReplayTestData.evaluatedAt,

            scheduleRevision = 0,
        )

    /**
     * 总是抛异常的 Snapshot Store，
     * 用来模拟"数据库里的历史损坏 / 版本不兼容"。
     */
    private class ExplodingSnapshotStore :
        DynamicScheduleDecisionSnapshotStore {

        override suspend fun save(
            snapshot: DynamicScheduleDecisionSnapshot,
        ) = Unit

        override suspend fun get(
            snapshotId: DynamicScheduleDecisionSnapshotId,
        ): DynamicScheduleDecisionSnapshot? = null

        override suspend fun findLatestByItemId(
            itemId: DynamicScheduleItemId,
        ): DynamicScheduleDecisionSnapshot =
            throw IllegalStateException(
                "corrupted snapshot row",
            )

        /**
         * 16.32：批量版本同样抛。
         *
         * 如果这里返回空列表，这个测试替身就**不再模拟损坏**——
         * 调用方会看到"一条记录都没有"（一个合法状态），
         * 而不是"数据库坏了"。
         */
        override suspend fun findLatestByItemIds(
            itemIds: Collection<DynamicScheduleItemId>,
        ): List<DynamicScheduleDecisionSnapshot> =
            throw IllegalStateException(
                "corrupted snapshot row",
            )

        override suspend fun delete(
            snapshotId: DynamicScheduleDecisionSnapshotId,
        ) = Unit

        override suspend fun clear() = Unit

        override suspend fun query(
            query: DynamicScheduleDecisionSnapshotQuery,
        ): DynamicScheduleDecisionSnapshotQueryResult =
            DynamicScheduleDecisionSnapshotQueryResult(
                snapshots = emptyList(),
                totalMatched = 0,
            )
    }

    /**
     * 读取总是失败、写入正常的 Trace Store。
     */
    private class ExplodingTraceStore :
        DynamicScheduleDecisionTraceStore {

        override suspend fun save(
            record: DynamicScheduleDecisionTraceRecord,
        ) = Unit

        override suspend fun get(
            traceId: DynamicScheduleDecisionTraceId,
        ): DynamicScheduleDecisionTraceRecord =
            throw IllegalStateException(
                "corrupted trace row",
            )

        override suspend fun delete(
            traceId: DynamicScheduleDecisionTraceId,
        ) = Unit

        override suspend fun clear() = Unit

        override suspend fun query(
            query: DynamicScheduleDecisionTraceQuery,
        ): DynamicScheduleDecisionTraceQueryResult =
            DynamicScheduleDecisionTraceQueryResult(
                records = emptyList(),
                totalMatched = 0,
            )
    }
}
