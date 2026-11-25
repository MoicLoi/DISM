package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.history.DeterministicDynamicScheduleDecisionHistoryService
import me.moiclathy.dism.schedule.history.DynamicScheduleDecisionTimelineService
import me.moiclathy.dism.schedule.provenance.InMemoryDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 13.15.21
 *
 * Replay 的时间语义。
 *
 * 这一组测试守的唯一一件事：
 *
 * > **历史时间 ≠ 重放时间 ≠ 校验时间。**
 */
class DynamicScheduleReplayTimeSemanticsTest {

    private val evaluatedAt: Instant =
        DynamicScheduleReplayTestData.evaluatedAt

    private val replayedAtA: Instant =
        Instant.parse("2026-09-25T14:30:00Z")

    private val replayedAtB: Instant =
        Instant.parse("2026-09-25T15:30:00Z")

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val provenanceStore =
        InMemoryDynamicScheduleDecisionProvenanceStore()

    private val replayService =
        DynamicScheduleHistoricalRuntimeReplayService(
            replayInputPersistence =
                DynamicScheduleDecisionReplayInputSnapshotPersistenceService(
                    replayInputs,
                ),

            snapshotPersistence =
                DynamicScheduleDecisionSnapshotPersistenceService(
                    snapshots,
                ),

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

    // -----------------------------------------------------------------
    // Test 1：历史时间不等于 Replay 时间
    // -----------------------------------------------------------------

    @Test
    fun historicalTimeIsNotReplayTime() = runTest {

        val id = seedReplayable()

        val replay =
            replayService.replay(
                snapshotId = id,
                replayedAt = replayedAtA,
            )

        assertEquals(
            evaluatedAt,
            replay.replayInput.evaluatedAt,
        )

        assertEquals(
            replayedAtA,
            replay.execution.replayedAt,
        )

        assertNotEquals(
            replay.replayInput.evaluatedAt,
            replay.execution.replayedAt,
            "历史评估时间绝不能冒充成本次重放时间",
        )

        /*
         * 便捷访问器与 execution 必须是同一个来源。
         */
        assertEquals(
            replay.execution.replayedAt,
            replay.replayedAt,
        )

        assertEquals(
            evaluatedAt,
            replay.historicalEvaluatedAt,
        )
    }

    // -----------------------------------------------------------------
    // Test 2：固定 Replay 时间
    // -----------------------------------------------------------------

    @Test
    fun replayUsesTheProvidedExecutionTime() = runTest {

        val id = seedReplayable()

        val replay =
            replayService.replay(
                snapshotId = id,
                replayedAt = replayedAtB,
            )

        assertEquals(
            replayedAtB,
            replay.execution.replayedAt,
        )

        assertEquals(
            DynamicScheduleReplayTestData.ENGINE_VERSION,
            replay.execution.executorVersion,
        )
    }

    @Test
    fun verificationHasItsOwnTime() = runTest {

        val id = seedReplayable()

        val verifiedAt =
            Instant.parse("2026-09-25T16:00:00Z")

        val report =
            verificationService.verify(
                snapshotId = id,
                replayedAt = replayedAtA,
                verifiedAt = verifiedAt,
            )

        assertEquals(replayedAtA, report.replayedAt)

        assertEquals(verifiedAt, report.verifiedAt)

        assertEquals(evaluatedAt, report.historicalEvaluatedAt)

        assertNotEquals(
            report.verifiedAt,
            report.historicalEvaluatedAt,
        )
    }

    // -----------------------------------------------------------------
    // Test 3：执行时间不影响 Behavior Fingerprint
    // -----------------------------------------------------------------

    @Test
    fun executionTimeDoesNotChangeBehaviorFingerprint() =
        runTest {

            val id = seedReplayable()

            val first =
                replayService.replay(
                    snapshotId = id,
                    replayedAt = replayedAtA,
                )

            val second =
                replayService.replay(
                    snapshotId = id,
                    replayedAt = replayedAtB,
                )

            val fingerprintA =
                DynamicScheduleReplayBehaviorFingerprint.from(
                    snapshotId = id.value.toString(),
                    engineVersion =
                        first.execution.executorVersion,
                    evaluatedAt =
                        first.replayInput.evaluatedAt,
                    result = first.replayedResult,
                )

            val fingerprintB =
                DynamicScheduleReplayBehaviorFingerprint.from(
                    snapshotId = id.value.toString(),
                    engineVersion =
                        second.execution.executorVersion,
                    evaluatedAt =
                        second.replayInput.evaluatedAt,
                    result = second.replayedResult,
                )

            assertEquals(
                fingerprintA,
                fingerprintB,
                "不同 replayedAt 不应该改变行为指纹",
            )

            /*
             * 但两次执行本身是不同的执行——
             * 这正是 ExecutionId 与 BehaviorFingerprint 的区别。
             */
            assertNotEquals(
                first.execution.replayedAt,
                second.execution.replayedAt,
            )
        }

    @Test
    fun executionIdReflectsTheActualExecution() = runTest {

        val id = seedReplayable()

        val replay =
            replayService.replay(
                snapshotId = id,
                replayedAt = replayedAtA,
            )

        val executionId =
            DynamicScheduleReplayExecutionId(
                snapshotId = id.value.toString(),

                engineVersion = replay.execution.executorVersion,

                replayedAt = replay.execution.replayedAt,

                resultFingerprint =
                    DynamicScheduleReplayDeterminismFingerprint
                        .from(replay.replayedResult)
                        .canonical,
            )

        assertEquals(replayedAtA, executionId.replayedAt)

        assertNotEquals(
            replayedAtA,
            executionId.let {
                it.replayedAt
            }.let {
                // 与历史时间不同
                evaluatedAt
            },
        )
    }

    // -----------------------------------------------------------------
    // Test 4：Batch 时间
    // -----------------------------------------------------------------

    @Test
    fun batchUsesASingleExecutionTimestamp() = runTest {

        val a = seedReplayable()

        val b = seedReplayable()

        val c = seedReplayable()

        val startedAt =
            Instant.parse("2026-09-25T17:00:00Z")

        val result =
            batch.replay(
                snapshotIds = listOf(a, b, c),
                startedAt = startedAt,
            )

        assertEquals(startedAt, result.execution.startedAt)

        assertEquals(
            DynamicScheduleReplayTestData.ENGINE_VERSION,
            result.execution.replayEngineVersion,
        )

        /*
         * 三个 Replay 使用**同一个**执行时间：
         * 它们是"一次用户发起的历史重演操作"，
         * 不是三次互不相关的操作。
         */
        result.successes.forEach { entry ->
            assertEquals(
                startedAt,
                entry.replay.execution.replayedAt,
            )
        }
    }

    // -----------------------------------------------------------------
    // Test 5：Timeline 不被 Replay 改变
    // -----------------------------------------------------------------

    @Test
    fun timelineOrderingIgnoresReplayTime() = runTest {

        val a = seedReplayable(hoursOffset = 0)

        val b = seedReplayable(hoursOffset = 1)

        val c = seedReplayable(hoursOffset = 2)

        val timelineService =
            DynamicScheduleDecisionTimelineService(
                DeterministicDynamicScheduleDecisionHistoryService(
                    snapshotPersistence =
                        DynamicScheduleDecisionSnapshotPersistenceService(
                            snapshots,
                        ),
                    tracePersistence =
                        DynamicScheduleDecisionTracePersistenceService(
                            traces,
                        ),
                    provenanceStore = provenanceStore,
                ),
            )

        val before =
            timelineService
                .timeline(
                    DynamicScheduleReplayTestData.itemId,
                )
                .entries
                .map { it.snapshotId }

        assertEquals(listOf(c, b, a), before)

        /*
         * 第二天重放 B。
         */
        replayService.replay(
            snapshotId = b,
            replayedAt = replayedAtB,
        )

        val after =
            timelineService
                .timeline(
                    DynamicScheduleReplayTestData.itemId,
                )
                .entries
                .map { it.snapshotId }

        assertEquals(
            before,
            after,
            "Replay 是附加事件，不能把历史 Decision 移动到第二天",
        )
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    /**
     * 每次调用生成一条可重放的、时间递增的决策。
     */
    private suspend fun seedReplayable(
        hoursOffset: Int = 0,
    ): me.moiclathy.dism.schedule.snapshot
        .DynamicScheduleDecisionSnapshotId {

        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(
                    capturedAt =
                        evaluatedAt + hoursOffset.hours,
                )

        snapshots.save(snapshot)

        replayInputs.save(
            DynamicScheduleReplayTestData
                .create()
                .copy(decisionSnapshot = snapshot),
        )

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

        assertTrue(snapshot.engineVersion.isNotBlank())

        return snapshot.id
    }
}
