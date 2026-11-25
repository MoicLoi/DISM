package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.COMPLETION_TENDENCY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.HIGH_DECISION_STABILITY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.LOW_DECISION_STABILITY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.NOT_RECENTLY_ACTIVE
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.POSTPONEMENT_TENDENCY
import me.moiclathy.dism.schedule.adaptation.DynamicScheduleDecisionInterpretationType.RECENTLY_ACTIVE
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.3
 *
 * Interpretation → Behavioral Adaptation Model。
 *
 * 这一层仍然**不改任何决策**，只把倾向整理成模型。
 */
class DynamicScheduleBehavioralAdaptationTest {

    private val itemId =
        DynamicScheduleItemId("test-item")

    private val builder =
        DeterministicDynamicScheduleBehavioralAdaptationBuilder()

    private fun signal(
        type: DynamicScheduleDecisionInterpretationType,
        strength: Double,
        confidence: Double = 0.8,
        sampleCount: Int = 10,
        sourceFeatureKey: String = type.name.lowercase(),
    ) =
        DynamicScheduleDecisionInterpretationSignal(
            type = type,
            strength = strength,
            confidence = confidence,
            sampleCount = sampleCount,
            sourceFeatureKey = sourceFeatureKey,
        )

    private fun interpretationOf(
        vararg signals: DynamicScheduleDecisionInterpretationSignal,
    ) =
        DynamicScheduleDecisionInterpretation(
            signals = signals.toList(),
        )

    // -----------------------------------------------------------------
    // 单向倾向
    // -----------------------------------------------------------------

    @Test
    fun completionTendencyIsPreserved() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(COMPLETION_TENDENCY, 0.82),
                    ),
            )

        assertEquals(0.82, result.completionTendency)

        assertEquals(10, result.sampleCount)

        assertTrue(result.hasEvidence)

        assertTrue(result.hasTendencies)
    }

    @Test
    fun postponementAndCancellationArePreserved() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(POSTPONEMENT_TENDENCY, 0.45),
                        signal(
                            DynamicScheduleDecisionInterpretationType
                                .CANCELLATION_TENDENCY,
                            0.33,
                        ),
                    ),
            )

        assertEquals(0.45, result.postponementTendency)

        assertEquals(0.33, result.cancellationTendency)

        assertEquals(0.0, result.completionTendency)
    }

    // -----------------------------------------------------------------
    // 双向倾向还原
    // -----------------------------------------------------------------

    @Test
    fun lowStabilityIsConvertedBackToStability() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(LOW_DECISION_STABILITY, 0.75),
                    ),
            )

        assertEquals(
            0.25,
            result.decisionStability,
            0.000001,
            "反向语义必须还原成真正的 stability",
        )
    }

    @Test
    fun highStabilityIsPreserved() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(HIGH_DECISION_STABILITY, 0.91),
                    ),
            )

        assertEquals(0.91, result.decisionStability)
    }

    @Test
    fun notRecentlyActiveIsConvertedBackToActivity() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(NOT_RECENTLY_ACTIVE, 0.80),
                    ),
            )

        assertEquals(
            0.20,
            result.recentActivity,
            0.000001,
        )
    }

    @Test
    fun recentlyActiveIsPreserved() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(RECENTLY_ACTIVE, 0.76),
                    ),
            )

        assertEquals(0.76, result.recentActivity)
    }

    // -----------------------------------------------------------------
    // 置信度：样本加权
    // -----------------------------------------------------------------

    @Test
    fun confidenceUsesSampleWeightedAverage() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(
                            COMPLETION_TENDENCY,
                            strength = 0.9,
                            confidence = 1.0,
                            sampleCount = 1,
                        ),
                        signal(
                            HIGH_DECISION_STABILITY,
                            strength = 0.9,
                            confidence = 0.5,
                            sampleCount = 9,
                        ),
                    ),
            )

        /*
         * (1.0 * 1 + 0.5 * 9) / 10 = 0.55
         *
         * 简单平均会得到 0.75 ——
         * 那等于让"一次观察"和"九次观察"一样有分量。
         */
        assertEquals(
            0.55,
            result.confidence,
            0.000001,
        )
    }

    @Test
    fun sampleCountTakesTheLargestEvidenceWindow() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(
                            COMPLETION_TENDENCY,
                            0.9,
                            sampleCount = 3,
                        ),
                        signal(
                            POSTPONEMENT_TENDENCY,
                            0.4,
                            sampleCount = 12,
                        ),
                    ),
            )

        assertEquals(12, result.sampleCount)
    }

    // -----------------------------------------------------------------
    // 空输入
    // -----------------------------------------------------------------

    @Test
    fun emptyInterpretationProducesEmptyBehavioralEvidence() {

        val result =
            builder.build(
                itemId = itemId,
                interpretation =
                    DynamicScheduleDecisionInterpretation.EMPTY,
            )

        assertFalse(result.hasEvidence)

        assertFalse(result.isReliable)

        assertFalse(result.hasTendencies)

        assertEquals(0.0, result.confidence)

        assertEquals(0, result.sampleCount)

        assertEquals(0.0, result.decisionStability)

        assertEquals(0.0, result.recentActivity)
    }

    // -----------------------------------------------------------------
    // 可靠性
    // -----------------------------------------------------------------

    @Test
    fun reliableModelRequiresEvidenceAndConfidence() {

        val reliable =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(
                            COMPLETION_TENDENCY,
                            strength = 0.9,
                            confidence = 0.8,
                            sampleCount = 10,
                        ),
                    ),
            )

        assertTrue(reliable.hasEvidence)

        assertTrue(reliable.isReliable)
    }

    @Test
    fun lowConfidenceIsNotReliable() {

        val weak =
            builder.build(
                itemId = itemId,
                interpretation =
                    interpretationOf(
                        signal(
                            COMPLETION_TENDENCY,
                            strength = 0.9,
                            confidence = 0.2,
                            sampleCount = 2,
                        ),
                    ),
            )

        assertTrue(weak.hasEvidence)

        assertFalse(
            weak.isReliable,
            "样本少时即便有证据也不能算可靠",
        )
    }

    // -----------------------------------------------------------------
    // 确定性
    // -----------------------------------------------------------------

    @Test
    fun adaptationIsDeterministic() {

        val interpretation =
            interpretationOf(
                signal(COMPLETION_TENDENCY, 0.82),
                signal(POSTPONEMENT_TENDENCY, 0.12),
                signal(HIGH_DECISION_STABILITY, 0.91),
                signal(RECENTLY_ACTIVE, 0.76),
            )

        assertEquals(
            builder.build(itemId, interpretation),
            builder.build(itemId, interpretation),
        )
    }

    // -----------------------------------------------------------------
    // Service / Set
    // -----------------------------------------------------------------

    @Test
    fun serviceDelegatesToTheBuilder() {

        val interpretation =
            interpretationOf(
                signal(COMPLETION_TENDENCY, 0.82),
            )

        val service =
            DynamicScheduleBehavioralAdaptationService()

        assertEquals(
            builder.build(itemId, interpretation),
            service.build(itemId, interpretation),
        )
    }

    @Test
    fun setBuilderOrdersByItemId() {

        val other =
            DynamicScheduleItemId("aaa-item")

        val set =
            DynamicScheduleBehavioralAdaptationSetBuilder()
                .build(
                    mapOf(
                        itemId to
                            interpretationOf(
                                signal(
                                    COMPLETION_TENDENCY,
                                    0.9,
                                ),
                            ),

                        other to
                            interpretationOf(
                                signal(
                                    COMPLETION_TENDENCY,
                                    0.5,
                                ),
                            ),
                    ),
                )

        assertEquals(
            listOf("aaa-item", "test-item"),
            set.adaptations.map { it.itemId.value },
        )

        assertNotNull(set.find(other))

        assertNull(
            set.find(DynamicScheduleItemId("missing")),
        )
    }

    @Test
    fun emptySignalSetIsNotReliableButWithEvidenceFiltersIt() {

        val set =
            DynamicScheduleBehavioralAdaptationSet(
                adaptations =
                    listOf(
                        builder.build(
                            itemId,
                            DynamicScheduleDecisionInterpretation
                                .EMPTY,
                        ),
                    ),
            )

        assertEquals(1, set.size)

        assertTrue(set.withEvidence.isEmpty())
    }

    @Test
    fun setRejectsDuplicateItems() {

        val adaptation =
            builder.build(
                itemId,
                interpretationOf(
                    signal(COMPLETION_TENDENCY, 0.9),
                ),
            )

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DynamicScheduleBehavioralAdaptationSet(
                adaptations = listOf(adaptation, adaptation),
            )
        }
    }

    @Test
    fun invalidValuesAreRejected() {

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DynamicScheduleBehavioralAdaptation(
                itemId = itemId,
                completionTendency = 1.5,
            )
        }

        kotlin.test.assertFailsWith<IllegalArgumentException> {
            DynamicScheduleBehavioralAdaptation(
                itemId = itemId,
                confidence = -0.1,
            )
        }
    }
}
