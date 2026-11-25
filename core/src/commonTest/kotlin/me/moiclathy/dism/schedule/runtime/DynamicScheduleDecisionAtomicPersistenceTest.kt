package me.moiclathy.dism.schedule.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.hours

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshot
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.6
 *
 * 原子的含义：
 *
 * ```text
 * 正常：Snapshot ✓  Trace ✓  ReplayInput ✓
 * 异常：Snapshot ✗  Trace ✗  ReplayInput ✗
 * ```
 *
 * 最关键的测试是 [replayInputFailureRollsBackTraceAndSnapshot]：
 * 它证明"写了一半的历史"不会留在存储里。
 */
class DynamicScheduleDecisionAtomicPersistenceTest {

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    // -----------------------------------------------------------------
    // 测试 A：正常情况下三份都在
    // -----------------------------------------------------------------

    @Test
    fun atomicPersistenceWritesAllDecisionArtifacts() = runTest {

        val replayInputs =
            InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

        val fixture = fixture()

        atomic(replayInputs).persist(
            trace = fixture.trace,
            snapshot = fixture.snapshot,
            replayInput = fixture.replayInput,
        )

        assertNotNull(
            snapshots.get(fixture.snapshot.id),
        )

        assertNotNull(
            traces.get(fixture.trace.traceId),
        )

        assertNotNull(
            replayInputs.get(fixture.snapshot.id),
        )
    }

    // -----------------------------------------------------------------
    // 测试 B：ReplayInput 失败必须回滚前两项
    // -----------------------------------------------------------------

    @Test
    fun replayInputFailureRollsBackTraceAndSnapshot() = runTest {

        val replayInputs =
            FailingReplayInputStore(
                InMemoryDynamicScheduleDecisionReplayInputSnapshotStore(),
            )

        val fixture = fixture()

        assertFailsWith<IllegalStateException> {
            atomic(replayInputs).persist(
                trace = fixture.trace,
                snapshot = fixture.snapshot,
                replayInput = fixture.replayInput,
            )
        }

        assertNull(
            snapshots.get(fixture.snapshot.id),
            "Snapshot 必须被回滚",
        )

        assertNull(
            traces.get(fixture.trace.traceId),
            "Trace 必须被回滚",
        )

        assertNull(
            replayInputs.get(fixture.snapshot.id),
        )
    }

    @Test
    fun snapshotFailureRollsBackTrace() = runTest {

        val replayInputs =
            InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

        val persistence =
            InMemoryDynamicScheduleDecisionAtomicPersistence(
                snapshotStore = FailingSnapshotStore(snapshots),
                traceStore = traces,
                replayInputStore = replayInputs,
            )

        val fixture = fixture()

        assertFailsWith<IllegalStateException> {
            persistence.persist(
                trace = fixture.trace,
                snapshot = fixture.snapshot,
                replayInput = fixture.replayInput,
            )
        }

        assertNull(
            traces.get(fixture.trace.traceId),
            "Snapshot 写失败时 Trace 不能留在存储里",
        )

        assertNull(
            replayInputs.get(fixture.snapshot.id),
        )
    }

    // -----------------------------------------------------------------
    // 覆盖已有数据时的回滚语义
    // -----------------------------------------------------------------

    @Test
    fun rollbackRestoresPreviousValuesWhenOverwriting() =
        runTest {

            val replayInputs =
                InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

            val next = fixture()

            /*
             * 先写入同一 id 的"旧版本"（内容不同），
             * 这样回滚如果不能恢复，断言就会失败。
             */
            val previous = previousVersionOf(next)

            snapshots.save(previous.snapshot)

            traces.save(previous.trace)

            replayInputs.save(previous.replayInput)

            val persistence =
                InMemoryDynamicScheduleDecisionAtomicPersistence(
                    snapshotStore = snapshots,
                    traceStore = traces,
                    replayInputStore =
                        FailingReplayInputStore(replayInputs),
                )

            assertFailsWith<IllegalStateException> {
                persistence.persist(
                    trace = next.trace,
                    snapshot = next.snapshot,
                    replayInput = next.replayInput,
                )
            }

            /*
             * 回滚不是"删掉"，而是"恢复成覆盖之前的样子"。
             */
            assertEquals(
                previous.snapshot,
                snapshots.get(next.snapshot.id),
            )

            assertEquals(
                previous.trace,
                traces.get(next.trace.traceId),
            )

            assertEquals(
                previous.replayInput,
                replayInputs.get(next.snapshot.id),
            )
        }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private fun atomic(
        replayInputs:
            DynamicScheduleDecisionReplayInputSnapshotStore,
    ) =
        InMemoryDynamicScheduleDecisionAtomicPersistence(
            snapshotStore = snapshots,
            traceStore = traces,
            replayInputStore = replayInputs,
        )

    private class Fixture(
        val snapshot: DynamicScheduleDecisionSnapshot,

        val trace: DynamicScheduleDecisionTraceRecord,

        val replayInput:
            DynamicScheduleDecisionReplayInputSnapshot,
    )

    private fun fixture(): Fixture {

        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        /*
         * ReplayInput 必须与 Snapshot 共用同一个实例，
         * 因为两者通过 snapshot.id 关联。
         */
        val replayInput =
            DynamicScheduleReplayTestData
                .create()
                .copy(decisionSnapshot = snapshot)

        val trace =
            DynamicScheduleDecisionTraceRecord(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(
                            DynamicScheduleReplayTestData.itemId,
                        )
                        .copy(id = snapshot.traceId!!),

                createdAt = snapshot.capturedAt,

                scheduleRevision =
                    snapshot.scheduleRevision,
            )

        return Fixture(
            snapshot = snapshot,
            trace = trace,
            replayInput = replayInput,
        )
    }

    /**
     * 同一批 id、内容不同的更早版本。
     */
    private fun previousVersionOf(
        next: Fixture,
    ): Fixture {

        val snapshot =
            next.snapshot.copy(
                capturedAt =
                    next.snapshot.capturedAt - 1.hours,
            )

        val trace =
            next.trace.copy(
                createdAt = next.trace.createdAt - 1.hours,
            )

        val replayInput =
            next.replayInput.copy(
                decisionSnapshot = snapshot,

                evaluatedAt =
                    next.replayInput.evaluatedAt - 1.hours,
            )

        return Fixture(
            snapshot = snapshot,
            trace = trace,
            replayInput = replayInput,
        )
    }

    /**
     * 写入必定失败的 ReplayInput Store。
     */
    private class FailingReplayInputStore(
        private val delegate:
            DynamicScheduleDecisionReplayInputSnapshotStore,
    ) : DynamicScheduleDecisionReplayInputSnapshotStore by delegate {

        override suspend fun save(
            snapshot:
                DynamicScheduleDecisionReplayInputSnapshot,
        ) {
            throw IllegalStateException(
                "Injected replay-input failure",
            )
        }
    }

    /**
     * 写入必定失败的 Snapshot Store。
     */
    private class FailingSnapshotStore(
        private val delegate:
            DynamicScheduleDecisionSnapshotStore,
    ) : DynamicScheduleDecisionSnapshotStore by delegate {

        override suspend fun save(
            snapshot: DynamicScheduleDecisionSnapshot,
        ) {
            throw IllegalStateException(
                "Injected snapshot failure",
            )
        }
    }
}
