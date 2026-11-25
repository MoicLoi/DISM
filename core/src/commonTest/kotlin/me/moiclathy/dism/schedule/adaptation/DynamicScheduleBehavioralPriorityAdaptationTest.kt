package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleBehavioralPriorityAdaptationTest {

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

    private fun weightAdaptation(
        enabled: Boolean = true,
    ) =
        DynamicScheduleWeightAdaptation(
            itemId = itemId,

            deltas =
                DynamicScheduleWeightDimension
                    .entries
                    .associateWith { 0.1 },

            confidence = 0.8,
            sampleCount = 10,

            enabled = enabled,
        )

    @Test
    fun sufficientEvidenceEnablesPriorityAdaptation() {

        val result =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()
                .adapt(
                    behavioral =
                        behavioral(),

                    weightAdaptation =
                        weightAdaptation(),
                )

        assertTrue(result.enabled)
    }

    @Test
    fun insufficientSamplesDisableAdaptation() {

        val result =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            sampleCount = 2,
                        ),

                    weightAdaptation =
                        weightAdaptation(),
                )

        assertFalse(result.enabled)
        assertEquals(
            0.0,
            result.priorityDelta,
        )
    }

    @Test
    fun insufficientConfidenceDisablesAdaptation() {

        val result =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 0.49,
                        ),

                    weightAdaptation =
                        weightAdaptation(),
                )

        assertFalse(result.enabled)
    }

    @Test
    fun disabledWeightAdaptationDisablesPriorityAdaptation() {

        val result =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()
                .adapt(
                    behavioral =
                        behavioral(),

                    weightAdaptation =
                        weightAdaptation(
                            enabled = false,
                        ),
                )

        assertFalse(result.enabled)
        assertEquals(
            0.0,
            result.priorityDelta,
        )
    }

    @Test
    fun priorityDeltaIsScaledByConfidence() {

        val result =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 1.0,

                            completion = 0.0,
                            postponement = 1.0,
                            cancellation = 0.0,
                            stability = 0.0,
                            recentActivity = 0.0,
                        ),

                    weightAdaptation =
                        weightAdaptation(),
                )

        /*
         * postponement = 1.0
         * sensitivity = 0.75
         * confidence = 1.0
         * maximumDelta = 0.20
         *
         * => 0.15
         */
        assertEquals(
            0.15,
            result.priorityDelta,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun maximumDeltaIsRespected() {

        val result =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 1.0,
                            completion = 1.0,
                            postponement = 1.0,
                            cancellation = 1.0,
                            stability = 1.0,
                            recentActivity = 1.0,
                        ),

                    weightAdaptation =
                        weightAdaptation(),

                    policy =
                        DynamicScheduleBehavioralPriorityAdaptationPolicy(
                            maximumDelta = 0.10,
                        ),
                )

        assertEquals(
            0.10,
            result.priorityDelta,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun cancellationDoesNotAffectPriorityByDefault() {

        val result =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 1.0,

                            completion = 0.0,
                            postponement = 0.0,
                            cancellation = 1.0,
                            stability = 0.0,
                            recentActivity = 0.0,
                        ),

                    weightAdaptation =
                        weightAdaptation(),
                )

        assertEquals(
            0.0,
            result.priorityDelta,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun adaptationIsDeterministic() {

        val model =
            behavioral()

        val weight =
            weightAdaptation()

        val adapter =
            DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter()

        val first =
            adapter.adapt(
                behavioral = model,
                weightAdaptation = weight,
            )

        val second =
            adapter.adapt(
                behavioral = model,
                weightAdaptation = weight,
            )

        assertEquals(
            first,
            second,
        )
    }
}
