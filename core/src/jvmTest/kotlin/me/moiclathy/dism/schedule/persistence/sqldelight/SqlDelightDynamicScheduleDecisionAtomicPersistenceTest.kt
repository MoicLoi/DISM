package me.moiclathy.dism.schedule.persistence.sqldelight

import java.io.File

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.persistence.db.DismDatabase
import me.moiclathy.dism.persistence.driver.JvmDatabaseDriverFactory
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

/**
 * 13.6.11
 *
 * 真实的 SQLite 事务边界。
 *
 * 之前的 durability 测试证明的是：
 *
 * > 数据写进去以后，重启还能读。
 *
 * 这里要证明的是更强的一条：
 *
 * > **写入过程本身是一个不可被中途打断的历史事务。**
 *
 * ```text
 * 正常：Trace ✓  Snapshot ✓  ReplayInput ✓
 * 异常：Trace ✗  Snapshot ✗  ReplayInput ✗   ← 四张物理表都不留残余
 * ```
 */
class SqlDelightDynamicScheduleDecisionAtomicPersistenceTest {

    @Test
    fun successfulPersistWritesAllTables() = runTest {

        withDatabase { database, atomic, stores ->

            val fixture = fixture()

            atomic.persist(
                trace = fixture.trace,
                snapshot = fixture.snapshot,
                replayInput = fixture.replayInput,
            )

            assertNotNull(
                stores.snapshots.get(fixture.snapshot.id),
            )

            assertNotNull(
                stores.traces.get(fixture.trace.traceId),
            )

            assertNotNull(
                stores.replayInputs.get(fixture.snapshot.id),
            )
        }
    }

    @Test
    fun failingReplayInputRollsBackTheWholeDecision() =
        runTest {

            withDatabase(
                replayInputWriter = {
                    throw IllegalStateException(
                        "Injected replay-input failure",
                    )
                },
            ) { database, atomic, stores ->

                val fixture = fixture()

                assertFailsWith<IllegalStateException> {
                    atomic.persist(
                        trace = fixture.trace,
                        snapshot = fixture.snapshot,
                        replayInput = fixture.replayInput,
                    )
                }

                /*
                 * 三张逻辑表（四张物理表）都不能留下残余。
                 */
                assertNull(
                    stores.snapshots.get(fixture.snapshot.id),
                    "Snapshot 必须随事务一起回滚",
                )

                assertNull(
                    stores.traces.get(fixture.trace.traceId),
                    "Trace 必须随事务一起回滚",
                )

                assertNull(
                    stores.replayInputs.get(fixture.snapshot.id),
                )

                /*
                 * Trace Entry 也要一并回滚——
                 * 否则会留下永远查不到的孤儿行。
                 */
                assertEquals(
                    0,
                    database
                        .scheduleDecisionTraceQueries
                        .selectEntriesForTrace(
                            fixture.trace.traceId
                                .value
                                .toString(),
                        )
                        .executeAsList()
                        .size,
                    "Trace Entry 不能在回滚后残留",
                )
            }
        }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private class Stores(
        val snapshots:
            SqlDelightDynamicScheduleDecisionSnapshotStore,

        val traces:
            SqlDelightDynamicScheduleDecisionTraceStore,

        val replayInputs:
            SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore,
    )

    private class Fixture(
        val snapshot:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshot,

        val trace: DynamicScheduleDecisionTraceRecord,

        val replayInput:
            me.moiclathy.dism.schedule.replay
                .DynamicScheduleDecisionReplayInputSnapshot,
    )

    private suspend fun withDatabase(
        replayInputWriter:
            ((me.moiclathy.dism.schedule.replay
                .DynamicScheduleDecisionReplayInputSnapshot) -> Unit)?
            = null,

        body: suspend (
            DismDatabase,
            SqlDelightDynamicScheduleDecisionAtomicPersistence,
            Stores,
        ) -> Unit,
    ) {

        val file =
            File.createTempFile("dism-atomic-", ".db")

        file.delete()

        val driver =
            JvmDatabaseDriverFactory(file).createDriver()

        val database = DismDatabase(driver)

        val snapshots =
            SqlDelightDynamicScheduleDecisionSnapshotStore(
                database,
            )

        val traces =
            SqlDelightDynamicScheduleDecisionTraceStore(
                database,
            )

        val replayInputs =
            SqlDelightDynamicScheduleDecisionReplayInputSnapshotStore(
                database = database,
                snapshotStore = snapshots,
            )

        val atomic =
            if (replayInputWriter == null) {

                SqlDelightDynamicScheduleDecisionAtomicPersistence(
                    database = database,
                    snapshotStore = snapshots,
                    traceStore = traces,
                    replayInputStore = replayInputs,
                )

            } else {

                SqlDelightDynamicScheduleDecisionAtomicPersistence(
                    database = database,
                    writeSnapshot = snapshots::write,
                    writeTrace = traces::write,
                    writeReplayInput = replayInputWriter,
                )
            }

        try {

            body(
                database,
                atomic,
                Stores(
                    snapshots = snapshots,
                    traces = traces,
                    replayInputs = replayInputs,
                ),
            )

        } finally {

            driver.close()

            file.delete()
        }
    }

    private fun fixture(): Fixture {

        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

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
}
