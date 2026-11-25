package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.provenance.InMemoryDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.14.15
 *
 * 批量历史重放。
 *
 * 关键性质：
 *
 * ```text
 * 去重、上限、单项失败不毁掉整批、
 * continueOnFailure=false 时立即终止、
 * verify 不导致第二次重放
 * ```
 *
 * ### 为什么用真实服务而不是替身
 *
 * [`DynamicScheduleHistoricalRuntimeReplayService`] 是 final class，
 * 没法用子类替身；而这里也不需要替身——
 *
 * > **"缺少 ReplayInput"就是一个真实的失败路径。**
 *
 * 所以失败的制造方式是：给 A / C 写入 ReplayInput，
 * 但**不给 B**。B 的重放会真实地抛出 "replay input not found"。
 *
 * 同理，"某条历史到底有没有被重放"不用调用日志，
 * 而是看**来源链条数**——这是生产系统里真实可观察的证据。
 */
class DynamicScheduleHistoryReplayBatchServiceTest {

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val provenanceStore =
        InMemoryDynamicScheduleDecisionProvenanceStore()

    private val snapshotPersistence =
        DynamicScheduleDecisionSnapshotPersistenceService(
            snapshots,
        )

    private val replayService =
        DynamicScheduleHistoricalRuntimeReplayService(
            replayInputPersistence =
                DynamicScheduleDecisionReplayInputSnapshotPersistenceService(
                    replayInputs,
                ),

            snapshotPersistence = snapshotPersistence,

            tracePersistence =
                DynamicScheduleDecisionTracePersistenceService(
                    traces,
                ),

            replayEngine =
                DeterministicDynamicScheduleHistoricalRuntimeReplayEngine(
                    runtime =
                        DynamicScheduleReplayTestData.runtime(),
                ),

            replayEngineVersion =
                DynamicScheduleReplayTestData.ENGINE_VERSION,

            provenanceStore = provenanceStore,
        )

    private val verificationService =
        DynamicScheduleReplayVerificationService(
            replayService = replayService,

            replayEngineVersion =
                DynamicScheduleReplayTestData.ENGINE_VERSION,

            provenanceStore = provenanceStore,
        )

    private val batch =
        DynamicScheduleHistoryReplayBatchService(
            replayService = replayService,
            verificationService = verificationService,
        )

    /** 每次测试重建三个可重放的 id。 */
    private suspend fun newReplayableId():
        DynamicScheduleDecisionSnapshotId {
        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        snapshots.save(snapshot)

        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(decisionSnapshot = snapshot),
        )

        /*
         * 历史 Trace 必须包含 EXPLANATION 阶段，
         * 否则 12.28 的 explanation parity 会认为
         * "历史无解释 / 重放有解释"，把结论判成 BEHAVIOR_CHANGED。
         *
         * 那是夹具不完整，不是系统出错——所以在这里一次补好。
         */
        traces.save(
            DynamicScheduleDecisionTraceRecord(
                trace =
                    DeterministicDynamicScheduleDecisionTraceBuilder()
                        .create(
                            DynamicScheduleReplayTestData.itemId,
                        )
                        .copy(id = snapshot.traceId!!)
                        .withEntry(
                            stage =
                                DynamicScheduleDecisionTraceStage
                                    .OUTPUT,

                            message = "Output",

                            decisive = true,
                        )
                        .withEntry(
                            stage =
                                DynamicScheduleDecisionTraceStage
                                    .EXPLANATION,

                            message =
                                "Historical decision produced an explanation.",
                        ),

                createdAt = snapshot.capturedAt,

                scheduleRevision =
                    snapshot.scheduleRevision,

                engineVersion = snapshot.engineVersion,
            ),
        )

        return snapshot.id
    }

    /** 只有 Snapshot、没有 ReplayInput → 重放必然失败。 */
    private suspend fun newUnreplayableId():
        DynamicScheduleDecisionSnapshotId {
        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

        snapshots.save(snapshot)

        return snapshot.id
    }

    private suspend fun replayAttemptCount(): Int =
        provenanceStore
            .queryForItem(DynamicScheduleReplayTestData.itemId)
            .size

    // -----------------------------------------------------------------
    // 去重 / 上限
    // -----------------------------------------------------------------

    @Test
    fun duplicateIdsAreReplayedOnce() = runTest {

        val a = newReplayableId()

        val b = newReplayableId()

        val c = newReplayableId()

        val result =
            batch.replay(
                snapshotIds = listOf(a, b, a, c, b),
            )

        assertEquals(3, result.total)

        assertEquals(3, result.succeeded)

        assertTrue(result.isComplete)

        /*
         * 去重的证据：只有 3 次重放留下了来源链。
         * 若重复执行 5 次，这里会是 5。
         */
        assertEquals(3, replayAttemptCount())
    }

    @Test
    fun maxItemsBoundsTheBatch() = runTest {

        val ids =
            List(100) {
                DynamicScheduleDecisionSnapshotId()
            }

        val result =
            batch.replay(
                snapshotIds = ids,
                policy =
                    DynamicScheduleHistoryReplayBatchPolicy(
                        maxItems = 10,
                    ),
            )

        assertEquals(
            10,
            result.total,
            "100 个输入必须被 maxItems 截到 10",
        )

        assertEquals(ids.take(10), result.entries.map { it.snapshotId })
    }

    @Test
    fun emptyInputIsAnEmptyBatch() = runTest {

        val result = batch.replay(snapshotIds = emptyList())

        assertEquals(0, result.total)

        assertTrue(result.entries.isEmpty())

        assertEquals(0, replayAttemptCount())
    }

    // -----------------------------------------------------------------
    // 失败处理
    // -----------------------------------------------------------------

    @Test
    fun failuresDoNotDestroyTheWholeBatch() = runTest {

        val a = newReplayableId()

        val broken = newUnreplayableId()

        val c = newReplayableId()

        val result =
            batch.replay(
                snapshotIds = listOf(a, broken, c),
            )

        assertEquals(3, result.total)

        assertEquals(2, result.succeeded)

        assertEquals(1, result.failed)

        assertFalse(result.isComplete)

        assertEquals(
            broken,
            result.failures.single().snapshotId,
        )

        assertTrue(
            result.failures
                .single()
                .message
                .contains("replay input not found"),
        )

        /* 失败之后 C 仍然被重放了。 */
        assertEquals(2, replayAttemptCount())
    }

    @Test
    fun continueOnFailureFalseStopsTheBatch() = runTest {

        val a = newReplayableId()

        val broken = newUnreplayableId()

        val c = newReplayableId()

        assertFailsWith<IllegalStateException> {
            batch.replay(
                snapshotIds = listOf(a, broken, c),

                policy =
                    DynamicScheduleHistoryReplayBatchPolicy(
                        continueOnFailure = false,
                    ),
            )
        }

        /*
         * 顺序执行下 C 不应该被碰到：
         * 只留下了 A 那一条来源链。
         */
        assertEquals(
            1,
            replayAttemptCount(),
            "批次必须在第一个失败处终止，且不碰后续项目",
        )
    }

    // -----------------------------------------------------------------
    // Verification
    // -----------------------------------------------------------------

    @Test
    fun verificationIsOptionalAndDoesNotReplayTwice() = runTest {

        val id = newReplayableId()

        val withoutVerify =
            batch.replay(snapshotIds = listOf(id))

        assertNull(
            withoutVerify.successes.single().verification,
        )

        assertEquals(1, replayAttemptCount())

        val withVerify =
            batch.replay(
                snapshotIds = listOf(id),

                policy =
                    DynamicScheduleHistoryReplayBatchPolicy(
                        verify = true,
                    ),
            )

        val verification =
            assertNotNull(
                withVerify.successes.single().verification,
            )

        assertEquals(
            DynamicScheduleReplayVerificationStatus.EXACT_MATCH,
            verification.status,
        )

        /*
         * 关键：verify = true 只增加了一次重放，
         * 而不是两次（12.28 的 verify(id) 内部会再重放一次）。
         */
        assertEquals(
            2,
            replayAttemptCount(),
            "verify 不允许导致第二次重放",
        )
    }

    // -----------------------------------------------------------------
    // 结果顺序与并发
    // -----------------------------------------------------------------

    @Test
    fun resultsFollowInputOrderEvenWhenConcurrent() = runTest {

        val ids =
            listOf(
                newReplayableId(),
                newReplayableId(),
                newReplayableId(),
            )

        val concurrent =
            batch.replay(
                snapshotIds = ids,
                policy =
                    DynamicScheduleHistoryReplayBatchPolicy(
                        maxConcurrency = 3,
                    ),
            )

        assertEquals(
            ids,
            concurrent.entries.map { it.snapshotId },
            "并发只影响完成时机，不影响结果排列",
        )

        assertEquals(3, concurrent.succeeded)
    }

    @Test
    fun concurrencyKeepsFailuresInPlace() = runTest {

        val ids =
            listOf(
                newReplayableId(),
                newUnreplayableId(),
                newReplayableId(),
            )

        val result =
            batch.replay(
                snapshotIds = ids,
                policy =
                    DynamicScheduleHistoryReplayBatchPolicy(
                        maxConcurrency = 2,
                    ),
            )

        assertEquals(ids, result.entries.map { it.snapshotId })

        assertEquals(2, result.succeeded)

        assertEquals(1, result.failed)
    }

    @Test
    fun policyRejectsInvalidLimits() {

        assertFailsWith<IllegalArgumentException> {
            DynamicScheduleHistoryReplayBatchPolicy(maxItems = 0)
        }

        assertFailsWith<IllegalArgumentException> {
            DynamicScheduleHistoryReplayBatchPolicy(
                maxConcurrency = 0,
            )
        }
    }
}
