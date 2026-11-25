package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

import kotlinx.coroutines.test.runTest

import me.moiclathy.dism.schedule.history.DeterministicDynamicScheduleDecisionHistoryService
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.provenance.InMemoryDynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayTestData
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.InMemoryDynamicScheduleDecisionSnapshotStore
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.trace.InMemoryDynamicScheduleDecisionTraceStore

/**
 * 14.1
 *
 * 历史特征提取。
 *
 * 这一层只**测量**，不解释、不修改。
 */
class DynamicScheduleHistoricalFeatureExtractorTest {

    private val itemId =
        DynamicScheduleReplayTestData.itemId

    private val base: Instant =
        Instant.parse("2026-09-25T12:00:00Z")

    private val snapshots =
        InMemoryDynamicScheduleDecisionSnapshotStore()

    private val extractor =
        DeterministicDynamicScheduleHistoricalFeatureExtractor(
            history =
                DeterministicDynamicScheduleDecisionHistoryService(
                    snapshotPersistence =
                        DynamicScheduleDecisionSnapshotPersistenceService(
                            snapshots,
                        ),
                    tracePersistence =
                        DynamicScheduleDecisionTracePersistenceService(
                            InMemoryDynamicScheduleDecisionTraceStore(),
                        ),
                    provenanceStore =
                        InMemoryDynamicScheduleDecisionProvenanceStore(),
                ),
        )

    // -----------------------------------------------------------------
    // 1. 无历史
    // -----------------------------------------------------------------

    @Test
    fun noHistoryProducesAnEmptyFeatureSet() = runTest {

        val features =
            extractor.extract(
                itemId = itemId,
                limit = 50,
                now = base,
            )

        assertTrue(features.isEmpty)

        assertEquals(0, features.size)
    }

    // -----------------------------------------------------------------
    // 2 / 3 / 4. 比例特征
    // -----------------------------------------------------------------

    @Test
    fun completionRateMatchesTheWindow() = runTest {

        seed(
            completed = 8,
            postponed = 0,
            cancelled = 0,
            other = 2,
        )

        val features = extract(limit = 10)

        assertEquals(
            0.8,
            assertNotNull(
                features.value("completion_rate"),
            ),
            0.0001,
        )

        assertEquals(
            10,
            features.get("completion_rate")?.sampleCount,
        )
    }

    @Test
    fun postponementRateMatchesTheWindow() = runTest {

        seed(
            completed = 7,
            postponed = 3,
            cancelled = 0,
            other = 0,
        )

        assertEquals(
            0.3,
            assertNotNull(
                extract(limit = 10)
                    .value("postponement_rate"),
            ),
            0.0001,
        )
    }

    @Test
    fun cancellationRateMatchesTheWindow() = runTest {

        seed(
            completed = 8,
            postponed = 0,
            cancelled = 2,
            other = 0,
        )

        assertEquals(
            0.2,
            assertNotNull(
                extract(limit = 10)
                    .value("cancellation_rate"),
            ),
            0.0001,
        )
    }

    // -----------------------------------------------------------------
    // 5. Confidence
    // -----------------------------------------------------------------

    @Test
    fun confidenceGrowsWithSampleCount() = runTest {

        seed(
            completed = 1,
            postponed = 0,
            cancelled = 0,
            other = 0,
        )

        val few = assertNotNull(
            extract(limit = 10)
                .confidence("completion_rate"),
        )

        snapshots.clear()

        seed(
            completed = 20,
            postponed = 0,
            cancelled = 0,
            other = 0,
        )

        val many = assertNotNull(
            extract(limit = 50)
                .confidence("completion_rate"),
        )

        assertTrue(
            few < many,
            "1 个样本不能和 20 个样本一样可信（$few vs $many）",
        )

        /*
         * 值相同、可信度不同 —— 这正是 Value 与 Confidence
         * 必须分开的原因。
         */
        assertEquals(
            1.0,
            assertNotNull(
                extract(limit = 50).value("completion_rate"),
            ),
            0.0001,
        )
    }

    // -----------------------------------------------------------------
    // 6. Deterministic
    // -----------------------------------------------------------------

    @Test
    fun extractionIsDeterministic() = runTest {

        seed(
            completed = 6,
            postponed = 2,
            cancelled = 1,
            other = 1,
        )

        val first =
            extract(limit = 10)

        val second =
            extract(limit = 10)

        assertEquals(first, second)

        assertEquals(
            first.features.map { it.key },
            second.features.map { it.key },
        )
    }

    @Test
    fun extractionIsStableAcrossDifferentWallClockTimes() =
        runTest {

            seed(
                completed = 3,
                postponed = 0,
                cancelled = 0,
                other = 0,
            )

            /*
             * 特征值只依赖传入的 now，
             * 不依赖任何隐藏的系统时钟。
             */
            val atNoon =
                extractor.extract(
                    itemId = itemId,
                    limit = 10,
                    now = base,
                )

            val atMidnight =
                extractor.extract(
                    itemId = itemId,
                    limit = 10,
                    now = base + 12.hours,
                )

            assertEquals(
                atNoon.value("completion_rate"),
                atMidnight.value("completion_rate"),
            )

            /*
             * 但 recent_activity 必须变——它本来就是时效性的。
             */
            assertTrue(
                assertNotNull(
                    atMidnight.value("recent_activity"),
                ) <
                    assertNotNull(
                        atNoon.value("recent_activity"),
                    ),
            )
        }

    // -----------------------------------------------------------------
    // 稳定性
    // -----------------------------------------------------------------

    @Test
    fun stablePrioritiesYieldHighStability() = runTest {

        listOf(0.80, 0.81, 0.79, 0.82, 0.80).forEachIndexed {
                index,
                priority,
            ->
            saveSnapshot(
                state = DynamicScheduleState.UPCOMING,
                priority = priority,
                hoursAgo = index,
            )
        }

        val stability = assertNotNull(
            extract(limit = 10).value("decision_stability"),
        )

        assertTrue(
            stability > 0.9,
            "几乎不变的优先级应该非常稳定，实际 $stability",
        )
    }

    @Test
    fun volatilePrioritiesYieldLowStability() = runTest {

        listOf(0.90, 0.20, 0.85, 0.10, 0.92).forEachIndexed {
                index,
                priority,
            ->
            saveSnapshot(
                state = DynamicScheduleState.UPCOMING,
                priority = priority,
                hoursAgo = index,
            )
        }

        val stability = assertNotNull(
            extract(limit = 10).value("decision_stability"),
        )

        assertTrue(
            stability < 0.5,
            "剧烈波动的优先级应该明显不稳定，实际 $stability",
        )
    }

    @Test
    fun singleSampleIsConsideredStable() = runTest {

        seed(
            completed = 1,
            postponed = 0,
            cancelled = 0,
            other = 0,
        )

        assertEquals(
            1.0,
            assertNotNull(
                extract(limit = 10).value("decision_stability"),
            ),
            0.0001,
        )
    }

    // -----------------------------------------------------------------
    // 最近活跃度复用统一衰减
    // -----------------------------------------------------------------

    @Test
    fun recentActivityUsesTheSharedDecayFunction() = runTest {

        seed(
            completed = 1,
            postponed = 0,
            cancelled = 0,
            other = 0,
        )

        val observedAt =
            assertNotNull(
                extract(limit = 10).get("recent_activity"),
            ).observedAt

        val expected =
            DynamicScheduleRecencyDecay.weight(
                elapsed = base - observedAt,
                policy = DynamicScheduleAttentionDecayPolicy(),
            )

        assertEquals(
            expected,
            assertNotNull(
                extract(limit = 10).value("recent_activity"),
            ),
            0.0001,
            "recent_activity 必须与全项目共用的衰减公式一致",
        )
    }

    @Test
    fun ancientHistoryIsStillNotCompletelyForgotten() =
        runTest {

            saveSnapshot(
                state = DynamicScheduleState.COMPLETED,
                priority = 0.6,
                hoursAgo = 0,
            )

            val veryOld =
                extractor.extract(
                    itemId = itemId,
                    limit = 10,
                    now = base + 3650.days,
                )

            assertTrue(
                assertNotNull(
                    veryOld.value("recent_activity"),
                ) > 0.0,
                "老历史不应该完全归零",
            )
        }

    // -----------------------------------------------------------------
    // 来源标记
    // -----------------------------------------------------------------

    @Test
    fun featuresCarryTheirSource() = runTest {

        seed(
            completed = 2,
            postponed = 1,
            cancelled = 0,
            other = 0,
        )

        val features = extract(limit = 10)

        assertEquals(
            DynamicScheduleDecisionFeatureSource.SNAPSHOT_HISTORY,
            features.get("completion_rate")?.source,
        )

        assertEquals(
            DynamicScheduleDecisionFeatureSource.TRACE_HISTORY,
            features.get("decision_stability")?.source,
        )

        assertEquals(3, features.from(
            DynamicScheduleDecisionFeatureSource
                .SNAPSHOT_HISTORY,
        ).size - 1)
    }

    @Test
    fun limitBoundsTheWindow() = runTest {

        repeat(10) { index ->
            saveSnapshot(
                state = DynamicScheduleState.COMPLETED,
                priority = 0.6,
                hoursAgo = index,
            )
        }

        assertEquals(
            5,
            extract(limit = 5).get("completion_rate")?.sampleCount,
        )
    }

    @Test
    fun invalidLimitIsRejected() = runTest {

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            extractor.extract(
                itemId = itemId,
                limit = 0,
                now = base,
            )
        }
    }

    @Test
    fun historyIsNotModifiedByExtraction() = runTest {

        seed(
            completed = 3,
            postponed = 1,
            cancelled = 0,
            other = 1,
        )

        val before = allSnapshots()

        extract(limit = 10)

        extract(limit = 10)

        assertEquals(
            before,
            allSnapshots(),
            "特征提取是只读的，不能改动任何历史",
        )
    }

    // -----------------------------------------------------------------
    // helpers
    // -----------------------------------------------------------------

    private suspend fun extract(limit: Int) =
        extractor.extract(
            itemId = itemId,
            limit = limit,
            now = base,
        )

    private suspend fun allSnapshots():
        List<DynamicScheduleDecisionSnapshot> =
        snapshots
            .query(
                me.moiclathy.dism.schedule.snapshot
                    .DynamicScheduleDecisionSnapshotQuery(
                        itemId = itemId,
                        limit = Int.MAX_VALUE,
                    ),
            )
            .snapshots

    private suspend fun seed(
        completed: Int,
        postponed: Int,
        cancelled: Int,
        other: Int,
    ) {

        var hoursAgo = 0

        repeat(completed) {
            saveSnapshot(
                state = DynamicScheduleState.COMPLETED,
                priority = 0.6,
                hoursAgo = hoursAgo++,
            )
        }

        repeat(postponed) {
            saveSnapshot(
                state = DynamicScheduleState.POSTPONED,
                priority = 0.6,
                hoursAgo = hoursAgo++,
            )
        }

        repeat(cancelled) {
            saveSnapshot(
                state = DynamicScheduleState.CANCELLED,
                priority = 0.6,
                hoursAgo = hoursAgo++,
            )
        }

        repeat(other) {
            saveSnapshot(
                state = DynamicScheduleState.UPCOMING,
                priority = 0.6,
                hoursAgo = hoursAgo++,
            )
        }
    }

    private suspend fun saveSnapshot(
        state: DynamicScheduleState,

        priority: Double,

        hoursAgo: Int,
    ) {

        val snapshot =
            DynamicScheduleReplayTestData
                .decisionSnapshot()
                .copy(
                    id = DynamicScheduleDecisionSnapshotId(),

                    capturedAt = base - hoursAgo.hours,

                    item =
                        DynamicScheduleReplayTestData
                            .decisionSnapshot()
                            .item
                            .copy(
                                state = state,
                                priority = priority,
                            ),
                )

        assertFalse(snapshot.item.title.isBlank())

        snapshots.save(snapshot)
    }
}
