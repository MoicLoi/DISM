package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.CANCELLATION_TENDENCY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.COMPLETION_TENDENCY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.HIGH_DECISION_STABILITY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.LOW_DECISION_STABILITY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.NOT_RECENTLY_ACTIVE
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.POSTPONEMENT_TENDENCY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.RECENTLY_ACTIVE

/**
 * 14.2
 *
 * 特征 → 语义解释。
 *
 * 这一层只**解释**：既不改 Priority，也不给综合评分。
 */
class DynamicScheduleDecisionFeatureInterpreterTest {

    private val now: Instant =
        Instant.parse("2026-09-07T11:00:00Z")

    private val interpreter =
        DeterministicDynamicScheduleDecisionFeatureInterpreter()

    private fun feature(
        key: String,
        value: Double,
        confidence: Double = 0.8,
        sampleCount: Int = 10,
    ) =
        DynamicScheduleDecisionFeature(
            key = key,
            value = value,
            confidence = confidence,
            observedAt = now,
            sampleCount = sampleCount,
            source =
                DynamicScheduleDecisionFeatureSource
                    .SNAPSHOT_HISTORY,
        )

    private fun featuresOf(
        vararg features: DynamicScheduleDecisionFeature,
    ) =
        DynamicScheduleDecisionFeatureSet(
            features.toList(),
        )

    // -----------------------------------------------------------------
    // 阈值型语义
    // -----------------------------------------------------------------

    @Test
    fun highCompletionProducesCompletionTendency() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "completion_rate",
                        value = 0.85,
                    ),
                ),
            )

        val signal =
            assertNotNull(
                result.find(COMPLETION_TENDENCY),
            )

        assertEquals(0.85, signal.strength)

        assertEquals(0.8, signal.confidence)

        assertEquals(10, signal.sampleCount)

        assertEquals(
            "completion_rate",
            signal.sourceFeatureKey,
        )
    }

    @Test
    fun lowCompletionProducesNoTendency() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "completion_rate",
                        value = 0.40,
                    ),
                ),
            )

        assertNull(result.find(COMPLETION_TENDENCY))

        /*
         * 低于阈值时什么都不说——
         * "没有明显倾向"本身就是一种有意义的状态，
         * 不该被硬塞成"不完成倾向"。
         */
        assertTrue(result.isEmpty)
    }

    @Test
    fun thresholdBoundaryIsInclusive() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "completion_rate",
                        value = 0.70,
                    ),
                ),
            )

        assertNotNull(result.find(COMPLETION_TENDENCY))
    }

    @Test
    fun highPostponementProducesPostponementTendency() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "postponement_rate",
                        value = 0.45,
                    ),
                ),
            )

        assertEquals(
            0.45,
            assertNotNull(
                result.find(POSTPONEMENT_TENDENCY),
            ).strength,
        )
    }

    @Test
    fun highCancellationProducesCancellationTendency() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "cancellation_rate",
                        value = 0.35,
                    ),
                ),
            )

        assertNotNull(result.find(CANCELLATION_TENDENCY))
    }

    // -----------------------------------------------------------------
    // 双向语义
    // -----------------------------------------------------------------

    @Test
    fun highStabilityProducesHighStabilityOnly() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "decision_stability",
                        value = 0.91,
                    ),
                ),
            )

        assertNotNull(result.find(HIGH_DECISION_STABILITY))

        assertNull(result.find(LOW_DECISION_STABILITY))

        assertEquals(1, result.size)
    }

    @Test
    fun lowStabilityProducesLowStabilityWithComplementaryStrength() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "decision_stability",
                        value = 0.25,
                    ),
                ),
            )

        val signal =
            assertNotNull(result.find(LOW_DECISION_STABILITY))

        assertEquals(
            0.75,
            signal.strength,
            "反向语义的强度是补值",
        )

        assertNull(result.find(HIGH_DECISION_STABILITY))
    }

    @Test
    fun recentActivityProducesRecentlyActive() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "recent_activity",
                        value = 0.76,
                    ),
                ),
            )

        assertNotNull(result.find(RECENTLY_ACTIVE))

        assertNull(result.find(NOT_RECENTLY_ACTIVE))
    }

    @Test
    fun staleActivityProducesNotRecentlyActive() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "recent_activity",
                        value = 0.10,
                    ),
                ),
            )

        val signal =
            assertNotNull(result.find(NOT_RECENTLY_ACTIVE))

        assertEquals(0.90, signal.strength, 0.0001)

        assertNotNull(
            result.find(NOT_RECENTLY_ACTIVE),
        )
    }

    // -----------------------------------------------------------------
    // 空输入与确定性
    // -----------------------------------------------------------------

    @Test
    fun emptyFeatureSetProducesEmptyInterpretation() {

        val result =
            interpreter.interpret(
                DynamicScheduleDecisionFeatureSet.EMPTY,
            )

        assertTrue(result.isEmpty)

        assertEquals(0, result.size)
    }

    @Test
    fun interpretationIsDeterministic() {

        val features =
            featuresOf(
                feature("completion_rate", 0.82),
                feature("postponement_rate", 0.12),
                feature("cancellation_rate", 0.04),
                feature("decision_stability", 0.91),
                feature("recent_activity", 0.76),
            )

        assertEquals(
            interpreter.interpret(features),
            interpreter.interpret(features),
        )
    }

    @Test
    fun realisticFeatureSetProducesTheExpectedSignals() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature("completion_rate", 0.82),
                    feature("postponement_rate", 0.08),
                    feature("cancellation_rate", 0.03),
                    feature("decision_stability", 0.91),
                    feature("recent_activity", 0.76),
                ),
            )

        assertTrue(result.contains(COMPLETION_TENDENCY))

        assertTrue(result.contains(HIGH_DECISION_STABILITY))

        assertTrue(result.contains(RECENTLY_ACTIVE))

        /*
         * 低延后率 / 低取消率不产生"倾向"语义——
         * 只有超过阈值才说话。
         */
        assertFalse(result.contains(POSTPONEMENT_TENDENCY))

        assertFalse(result.contains(CANCELLATION_TENDENCY))

        assertEquals(3, result.size)
    }

    // -----------------------------------------------------------------
    // 弱样本
    // -----------------------------------------------------------------

    @Test
    fun strengthAndConfidenceStayIndependent() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature(
                        key = "completion_rate",
                        value = 1.0,
                        confidence = 0.1,
                        sampleCount = 1,
                    ),
                ),
            )

        val signal =
            assertNotNull(result.find(COMPLETION_TENDENCY))

        assertEquals(1.0, signal.strength)

        assertEquals(
            0.1,
            signal.confidence,
            "一次完成不能给出高置信度",
        )

        assertEquals(1, signal.sampleCount)
    }

    // -----------------------------------------------------------------
    // 服务层
    // -----------------------------------------------------------------

    @Test
    fun serviceDelegatesToTheInterpreter() {

        val service =
            DynamicScheduleDecisionInterpretationService()

        val features =
            featuresOf(
                feature("completion_rate", 0.9),
            )

        assertEquals(
            interpreter.interpret(features),
            service.interpret(features),
        )
    }

    @Test
    fun customThresholdsAreHonoured() {

        val strict =
            DeterministicDynamicScheduleDecisionFeatureInterpreter(
                completionThreshold = 0.95,
            )

        val features =
            featuresOf(
                feature("completion_rate", 0.90),
            )

        assertNotNull(
            interpreter
                .interpret(features)
                .find(COMPLETION_TENDENCY),
        )

        assertNull(
            strict
                .interpret(features)
                .find(COMPLETION_TENDENCY),
        )
    }

    @Test
    fun invalidThresholdsAreRejected() {

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DeterministicDynamicScheduleDecisionFeatureInterpreter(
                completionThreshold = 1.5,
            )
        }
    }

    @Test
    fun operatorGetAndGetAreEquivalent() {

        val features =
            featuresOf(
                feature("completion_rate", 0.9),
            )

        assertEquals(
            features.get("completion_rate"),
            features["completion_rate"],
        )
    }

    @Test
    fun missingFeatureProducesNoSignal() {

        val result =
            interpreter.interpret(
                featuresOf(
                    feature("recent_activity", 0.9),
                ),
            )

        assertNull(result.find(COMPLETION_TENDENCY))

        assertNull(result.find(HIGH_DECISION_STABILITY))

        assertEquals(1, result.size)
    }
}
