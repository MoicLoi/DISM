package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleWeightAdapterTest {

    private val itemId =
        DynamicScheduleItemId("test-item")

    private fun behavioral(
        confidence: Double = 0.8,
        sampleCount: Int = 10,
        completion: Double = 0.82,
        postponement: Double = 0.12,
        cancellation: Double = 0.03,
        stability: Double = 0.91,
        recentActivity: Double = 0.76,
    ) =
        DynamicScheduleBehavioralAdaptation(
            itemId = itemId,
            completionTendency = completion,
            postponementTendency = postponement,
            cancellationTendency = cancellation,
            decisionStability = stability,
            recentActivity = recentActivity,
            confidence = confidence,
            sampleCount = sampleCount,
        )

    @Test
    fun sufficientEvidenceEnablesAdaptation() {

        val result =
            DeterministicDynamicScheduleWeightAdapter()
                .adapt(
                    behavioral = behavioral(),
                )

        assertTrue(result.enabled)
    }

    @Test
    fun insufficientSamplesDisableAdaptation() {

        val result =
            DeterministicDynamicScheduleWeightAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            sampleCount = 2,
                        ),
                )

        assertFalse(result.enabled)

        DynamicScheduleWeightDimension
            .entries
            .forEach { dimension ->
                assertEquals(
                    0.0,
                    result.delta(dimension),
                )
            }
    }

    @Test
    fun insufficientConfidenceDisablesAdaptation() {

        val result =
            DeterministicDynamicScheduleWeightAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 0.49,
                        ),
                )

        assertFalse(result.enabled)
    }

    @Test
    fun completionWeightIsScaledByConfidence() {

        val result =
            DeterministicDynamicScheduleWeightAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            completion = 0.82,
                            confidence = 0.80,
                        ),
                )

        assertEquals(
            0.1312,
            result.delta(
                DynamicScheduleWeightDimension.COMPLETION,
            ),
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun maximumDeltaIsRespected() {

        val result =
            DeterministicDynamicScheduleWeightAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            completion = 1.0,
                            confidence = 1.0,
                        ),
                    policy =
                        DynamicScheduleWeightAdaptationPolicy(
                            maximumDelta = 0.10,
                        ),
                )

        assertEquals(
            0.10,
            result.delta(
                DynamicScheduleWeightDimension.COMPLETION,
            ),
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun zeroBehaviorProducesZeroDelta() {

        val result =
            DeterministicDynamicScheduleWeightAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            completion = 0.0,
                            postponement = 0.0,
                            cancellation = 0.0,
                            stability = 0.0,
                            recentActivity = 0.0,
                        ),
                )

        DynamicScheduleWeightDimension
            .entries
            .forEach { dimension ->
                assertEquals(
                    0.0,
                    result.delta(dimension),
                )
            }
    }

    @Test
    fun adaptationIsDeterministic() {

        val model =
            behavioral()

        val adapter =
            DeterministicDynamicScheduleWeightAdapter()

        val first =
            adapter.adapt(model)

        val second =
            adapter.adapt(model)

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun allDimensionsAreProduced() {

        val result =
            DeterministicDynamicScheduleWeightAdapter()
                .adapt(
                    behavioral(),
                )

        DynamicScheduleWeightDimension
            .entries
            .forEach { dimension ->
                assertTrue(
                    result.deltas.containsKey(dimension),
                )
            }
    }
}
