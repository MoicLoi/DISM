package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleAdaptationConfidenceTest {

    private val itemId =
        DynamicScheduleItemId("item-1")

    private fun adaptation(
        factor: Double = 1.20,
    ) =
        DynamicSchedulePriorityAdaptation(
            itemId = itemId,
            basePriority = 0.5,
            adaptationFactor = factor,
            effectivePriority =
                (0.5 * factor)
                    .coerceIn(0.0, 1.0),
        )

    @Test
    fun confidenceIncreasesWithEvidence() {

        val adapter =
            DeterministicDynamicScheduleAdaptationConfidenceAdapter()

        val low =
            adapter.calculate(
                input =
                    DynamicScheduleAdaptationConfidenceInput(
                        priorityAdaptation =
                            adaptation(),

                        historicalEvidence = 0.2,
                        stabilityEvidence = 0.2,
                        sampleCount = 1,
                    ),
            )

        val high =
            adapter.calculate(
                input =
                    DynamicScheduleAdaptationConfidenceInput(
                        priorityAdaptation =
                            adaptation(),

                        historicalEvidence = 0.9,
                        stabilityEvidence = 0.9,
                        sampleCount = 10,
                    ),
            )

        assertTrue(
            high.value > low.value,
        )
    }

    @Test
    fun sampleEvidenceSaturates() {

        val adapter =
            DeterministicDynamicScheduleAdaptationConfidenceAdapter()

        val result =
            adapter.calculate(
                input =
                    DynamicScheduleAdaptationConfidenceInput(
                        priorityAdaptation =
                            adaptation(),

                        historicalEvidence = 1.0,
                        stabilityEvidence = 1.0,
                        sampleCount = 10,
                    ),
            )

        assertEquals(
            1.0,
            result.sampleEvidence,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun zeroSamplesProduceZeroSampleEvidence() {

        val adapter =
            DeterministicDynamicScheduleAdaptationConfidenceAdapter()

        val result =
            adapter.calculate(
                input =
                    DynamicScheduleAdaptationConfidenceInput(
                        priorityAdaptation =
                            adaptation(),

                        historicalEvidence = 1.0,
                        stabilityEvidence = 1.0,
                        sampleCount = 0,
                    ),
            )

        assertEquals(
            0.0,
            result.sampleEvidence,
        )
    }

    @Test
    fun strongContextEvidenceRaisesConfidence() {

        val adapter =
            DeterministicDynamicScheduleAdaptationConfidenceAdapter()

        val withoutContext =
            adapter.calculate(
                input =
                    DynamicScheduleAdaptationConfidenceInput(
                        priorityAdaptation =
                            adaptation(),

                        historicalEvidence = 0.8,
                        stabilityEvidence = 0.8,
                        sampleCount = 10,
                    ),
            )

        val withContext =
            adapter.calculate(
                input =
                    DynamicScheduleAdaptationConfidenceInput(
                        priorityAdaptation =
                            adaptation(),

                        historicalEvidence = 0.8,
                        stabilityEvidence = 0.8,
                        sampleCount = 10,

                        contextInfluence =
                            testContextInfluence(
                                contextMatch = 1.0,
                            ),
                    ),
            )

        assertTrue(
            withContext.value >
                withoutContext.value,
        )
    }

    @Test
    fun lowConfidenceDisablesAdaptation() {

        val result =
            DeterministicDynamicScheduleAdaptationConfidenceAdapter()
                .calculate(
                    input =
                        DynamicScheduleAdaptationConfidenceInput(
                            priorityAdaptation =
                                adaptation(),

                            historicalEvidence = 0.1,
                            stabilityEvidence = 0.1,
                            sampleCount = 1,
                        ),

                    policy =
                        DynamicScheduleAdaptationConfidencePolicy(
                            minimumConfidence = 0.50,
                        ),
                )

        assertFalse(
            result.enabled,
        )

        assertTrue(
            result.value < 0.50,
        )
    }

    @Test
    fun highConfidenceEnablesAdaptation() {

        val result =
            DeterministicDynamicScheduleAdaptationConfidenceAdapter()
                .calculate(
                    input =
                        DynamicScheduleAdaptationConfidenceInput(
                            priorityAdaptation =
                                adaptation(),

                            historicalEvidence = 1.0,
                            stabilityEvidence = 1.0,
                            sampleCount = 10,
                        ),
                )

        assertTrue(
            result.enabled,
        )

        assertTrue(
            result.value >= 0.50,
        )
    }

    private fun testContextInfluence(
        contextMatch: Double,
    ) =
        me.moiclathy.dism.schedule.context
            .DynamicScheduleContextPriorityInfluence(
                itemId = itemId,
                contextFactor = 1.20,
                contextMatch = contextMatch,
                excluded = false,
                requiredSatisfied = true,
                matchedContextKeys =
                    setOf("after_class"),
            )
}
