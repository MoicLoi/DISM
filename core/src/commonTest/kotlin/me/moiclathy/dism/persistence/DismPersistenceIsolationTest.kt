package me.moiclathy.dism.persistence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.persistence.repository.InMemoryEntityRepository
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.3.17
 *
 * Persistence 隔离测试：**不碰数据库**。
 *
 * 它验证的是 13.3 的完成标准：
 *
 * ```text
 * Core
 *  │
 *  ├── 不知道 SQLDelight
 *  ├── 不知道 SqlDriver
 *  ├── 不知道具体数据库文件
 *  └── 不知道 InMemory 实现
 * ```
 *
 * 也就是说 `DismPersistence` 只是 Port Container，
 * 不带数据库也能构造、能工作，而且三个 Port 互相独立。
 */
class DismPersistenceIsolationTest {

    @Test
    fun defaultPersistenceIsUsableWithoutDatabase() {

        val persistence = DismPersistence()

        assertNotNull(persistence.scheduleSnapshots)
        assertNotNull(persistence.scheduleTraces)
        assertNotNull(persistence.scheduleReplayInputs)

        assertNotNull(persistence.entities)
        assertNotNull(persistence.evidence)
        assertNotNull(persistence.relationStates)
        assertNotNull(persistence.transitionLog)
        assertNotNull(persistence.stateCommit)
    }

    @Test
    fun defaultPortsAreInMemoryAdapters() {

        val persistence = DismPersistence()

        assertEquals(
            true,
            persistence.scheduleSnapshots
                is InMemoryDynamicScheduleDecisionSnapshotStore,
        )

        assertEquals(
            true,
            persistence.scheduleTraces
                is InMemoryDynamicScheduleDecisionTraceStore,
        )

        assertEquals(
            true,
            persistence.scheduleReplayInputs
                is InMemoryDynamicScheduleDecisionReplayInputSnapshotStore,
        )

        assertEquals(
            true,
            persistence.entities
                is InMemoryEntityRepository,
        )
    }

    @Test
    fun theThreeSchedulePortsAreIndependent() = runTest {

        val persistence = DismPersistence()

        /*
         * 注意：decisionSnapshot() / create() 每次都会生成新的随机 id，
         * 所以这里必须复用同一个实例，
         * 否则 get() 用的 id 与 save() 写的 id 不是同一个。
         */
        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        val replay =
            DynamicScheduleReplayTestData.create()

        persistence.scheduleSnapshots.save(snapshot)

        persistence.scheduleTraces.save(
            traceRecord(),
        )

        persistence.scheduleReplayInputs.save(replay)

        /*
         * 清空 Snapshot 不能影响 Trace / ReplayInput。
         */
        persistence.scheduleSnapshots.clear()

        assertNull(
            persistence.scheduleSnapshots.get(snapshot.id),
        )

        assertEquals(
            1,
            persistence.scheduleTraces
                .query(
                    me.moiclathy.dism.schedule.trace
                        .DynamicScheduleDecisionTraceQuery(),
                )
                .totalMatched,
        )

        assertNotNull(
            persistence.scheduleReplayInputs.get(
                replay.decisionSnapshot.id,
            ),
        )
    }

    @Test
    fun clearingOneSchedulePortDoesNotAffectTheOtherTwo() = runTest {

        val persistence = DismPersistence()

        val replay =
            DynamicScheduleReplayTestData.create()

        val snapshotId = replay.decisionSnapshot.id

        persistence.scheduleSnapshots.save(
            replay.decisionSnapshot,
        )

        persistence.scheduleTraces.save(
            traceRecord(),
        )

        persistence.scheduleReplayInputs.save(replay)

        /*
         * 清空 Trace 不能影响 Snapshot / ReplayInput。
         */
        persistence.scheduleTraces.clear()

        assertNotNull(
            persistence.scheduleSnapshots.get(snapshotId),
        )

        assertNotNull(
            persistence.scheduleReplayInputs.get(snapshotId),
        )

        /*
         * 清空 ReplayInput 不能影响 Snapshot。
         */
        persistence.scheduleReplayInputs.clear()

        assertNull(
            persistence.scheduleReplayInputs.get(snapshotId),
        )

        assertNotNull(
            persistence.scheduleSnapshots.get(snapshotId),
        )
    }

    @Test
    fun twoPersistenceContainersDoNotShareState() = runTest {

        val first = DismPersistence()

        val second = DismPersistence()

        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        first.scheduleSnapshots.save(snapshot)

        assertNotNull(
            first.scheduleSnapshots.get(snapshot.id),
        )

        assertNull(
            second.scheduleSnapshots.get(snapshot.id),
            "两个容器必须各自持有独立的 InMemory 实现",
        )
    }

    private fun traceRecord():
        DynamicScheduleDecisionTraceRecord =
        DynamicScheduleDecisionTraceRecord(
            trace =
                DeterministicDynamicScheduleDecisionTraceBuilder()
                    .create(
                        DynamicScheduleReplayTestData.itemId,
                    ),

            createdAt =
                DynamicScheduleReplayTestData.evaluatedAt,

            scheduleRevision = 0,
        )
}
