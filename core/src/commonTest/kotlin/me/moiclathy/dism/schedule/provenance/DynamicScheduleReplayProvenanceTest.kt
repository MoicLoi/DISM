package me.moiclathy.dism.schedule.provenance

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.replay.DeterministicDynamicScheduleHistoricalRuntimeReplayEngine
import me.moiclathy.dism.schedule.replay.DynamicScheduleDecisionReplayInputSnapshotPersistenceService
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayService
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayBehaviorFingerprint
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationService
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationStatus
import me.moiclathy.dism.schedule.replay.InMemoryDynamicScheduleDecisionReplayInputSnapshotStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder

/**
 * 13.16.20
 *
 * Replay Provenance 持久化。
 *
 * 四组测试对应"重启后还能回答的问题"：
 *
 * ```text
 * 这条历史被 Replay 过吗？
 * 上次什么时候？
 * 用哪个 Engine？
 * 行为指纹是什么？
 * 做过 Verification 吗？结果如何？
 * ```
 */
class DynamicScheduleReplayProvenanceTest {

    private val evaluatedAt =
        DynamicScheduleReplayTestData.evaluatedAt

    private val replayedAt: Instant =
        Instant.parse("2026-09-25T14:30:00Z")

    private val verifiedAt: Instant =
        Instant.parse("2026-09-25T14:31:00Z")

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val traces =
        InMemoryDynamicScheduleDecisionTraceStore()

    private val replayInputs =
        InMemoryDynamicScheduleDecisionReplayInputSnapshotStore()

    private val provenanceStore =
        InMemoryDynamicScheduleDecisionProvenanceStore()

    private fun replayService() =
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

    // -----------------------------------------------------------------
    // 1. Replay 会创建 Provenance
    // -----------------------------------------------------------------

    @Test
    fun replayCreatesProvenance() = runTest {

        val id = seedReplayable()

        assertNull(provenanceStore.latestForSnapshot(id))

        val replay =
            replayService().replay(
                snapshotId = id,
                replayedAt = replayedAt,
            )

        val provenance =
            assertNotNull(
                provenanceStore.latestForSnapshot(id),
            )

        assertEquals(replay.provenanceId, provenance.id)

        assertEquals(id, provenance.snapshotId)

        assertEquals(
            DynamicScheduleReplayTestData.ENGINE_VERSION,
            provenance.replayEngineVersion,
        )

        assertNull(
            provenance.verification,
            "Replay 刚结束时还没有校验过",
        )
    }

    // -----------------------------------------------------------------
    // 2. Replay 时间正确
    // -----------------------------------------------------------------

    @Test
    fun provenanceRecordsTheRealReplayTime() = runTest {

        val id = seedReplayable()

        replayService().replay(
            snapshotId = id,
            replayedAt = replayedAt,
        )

        val provenance =
            assertNotNull(
                provenanceStore.latestForSnapshot(id),
            )

        assertEquals(replayedAt, provenance.replayedAt)

        assertEquals(
            evaluatedAt,
            assertNotNull(
                provenanceReplayInputEvaluatedAt(id),
            ),
        )

        assertNotEquals(
            provenance.replayedAt,
            evaluatedAt,
            "历史评估时间绝不能冒充重放时间",
        )
    }

    // -----------------------------------------------------------------
    // 3. Behavior Fingerprint 被持久化，且不含 replayedAt
    // -----------------------------------------------------------------

    @Test
    fun behaviorFingerprintIsPersistedAndTimeIndependent() =
        runTest {

            val id = seedReplayable()

            val service = replayService()

            val first =
                service.replay(
                    snapshotId = id,
                    replayedAt = replayedAt,
                )

            val later =
                Instant.parse("2026-09-25T18:00:00Z")

            val second =
                service.replay(
                    snapshotId = id,
                    replayedAt = later,
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
                "执行时间不同不应该改变行为指纹",
            )

            /*
             * 两次重放各留下一条 provenance，
             * 最新那条对应第二次执行。
             */
            val latest =
                assertNotNull(
                    provenanceStore.latestForSnapshot(id),
                )

            assertEquals(later, latest.replayedAt)

            assertEquals(
                fingerprintB.fingerprint,
                latest.behaviorFingerprint,
            )
        }

    // -----------------------------------------------------------------
    // 4. Verification 会更新同一条 Provenance
    // -----------------------------------------------------------------

    @Test
    fun verificationBackfillsTheSameProvenance() = runTest {

        val id = seedReplayable()

        val replayService = replayService()

        val verificationService =
            DynamicScheduleReplayVerificationService(
                replayService = replayService,

                replayEngineVersion =
                    DynamicScheduleReplayTestData.ENGINE_VERSION,

                provenanceStore = provenanceStore,
            )

        val replay =
            replayService.replay(
                snapshotId = id,
                replayedAt = replayedAt,
            )

        val provenanceId =
            assertNotNull(replay.provenanceId)

        assertNull(
            assertNotNull(
                provenanceStore.get(provenanceId),
            ).verification,
        )

        verificationService.verify(
            replay = replay,
            verifiedAt = verifiedAt,
        )

        val updated =
            assertNotNull(provenanceStore.get(provenanceId))

        val verification =
            assertNotNull(updated.verification)

        assertEquals(
            DynamicScheduleReplayVerificationStatus.EXACT_MATCH,
            verification.status,
        )

        assertEquals(verifiedAt, verification.verifiedAt)

        assertEquals(0, verification.differenceCount)

        assertEquals(
            DynamicScheduleReplayTestData.ENGINE_VERSION,
            verification.replayEngineVersion,
        )

        /*
         * 回填不能动重放本身的事实。
         */
        assertEquals(replayedAt, updated.replayedAt)

        assertEquals(
            replay.behaviorFingerprintOrNull(),
            updated.behaviorFingerprint,
        )
    }

    // -----------------------------------------------------------------
    // 5. 同一条历史可以有多条 Provenance
    // -----------------------------------------------------------------

    @Test
    fun repeatedReplaysAccumulateProvenance() = runTest {

        val id = seedReplayable()

        val service = replayService()

        service.replay(snapshotId = id, replayedAt = replayedAt)

        service.replay(
            snapshotId = id,
            replayedAt =
                Instant.parse("2026-09-26T09:00:00Z"),
        )

        assertEquals(
            2,
            provenanceStore.queryForItem(
                DynamicScheduleReplayTestData.itemId,
            ).size,
        )

        assertEquals(
            Instant.parse("2026-09-26T09:00:00Z"),
            assertNotNull(
                provenanceStore.latestForSnapshot(id),
            ).replayedAt,
        )
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private suspend fun seedReplayable():
        me.moiclathy.dism.schedule.snapshot
            .DynamicScheduleDecisionSnapshotId {

        val snapshot =
            DynamicScheduleReplayTestData.decisionSnapshot()

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
                scheduleRevision = snapshot.scheduleRevision,
                engineVersion = snapshot.engineVersion,
            ),
        )

        return snapshot.id
    }

    private suspend fun provenanceReplayInputEvaluatedAt(
        snapshotId:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshotId,
    ): Instant? =
        replayInputs.get(snapshotId)?.evaluatedAt

    private fun DynamicScheduleHistoricalRuntimeReplayResult
        .behaviorFingerprintOrNull(): String? =
        DynamicScheduleReplayBehaviorFingerprint.from(
            snapshotId = replayInput.decisionSnapshot.id.value.toString(),
            engineVersion = execution.executorVersion,
            evaluatedAt = replayInput.evaluatedAt,
            result = replayedResult,
        ).fingerprint
}
