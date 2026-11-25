package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleAttentionAdapterTest {

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

    private fun weightAdaptation() =
        DynamicScheduleWeightAdaptation(
            itemId = itemId,
            deltas =
                DynamicScheduleWeightDimension
                    .entries
                    .associateWith { 0.1 },
            confidence = 0.8,
            sampleCount = 10,
            enabled = true,
        )

    @Test
    fun sufficientEvidenceEnablesAttentionAdaptation() {

        val result =
            DeterministicDynamicScheduleAttentionAdapter()
                .adapt(
                    behavioral = behavioral(),
                    weightAdaptation =
                        weightAdaptation(),
                )

        assertTrue(result.enabled)
    }

    @Test
    fun insufficientEvidenceDisablesAttentionAdaptation() {

        val result =
            DeterministicDynamicScheduleAttentionAdapter()
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
            result.attentionDelta,
        )
    }

    @Test
    fun disabledWeightAdaptationDisablesAttention() {

        val disabledWeight =
            weightAdaptation().copy(
                enabled = false,
            )

        val result =
            DeterministicDynamicScheduleAttentionAdapter()
                .adapt(
                    behavioral = behavioral(),
                    weightAdaptation =
                        disabledWeight,
                )

        assertFalse(result.enabled)
        assertEquals(
            0.0,
            result.attentionDelta,
        )
    }

    @Test
    fun recentActivityAndPostponementInfluenceAttention() {

        val result =
            DeterministicDynamicScheduleAttentionAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 1.0,
                            recentActivity = 0.8,
                            postponement = 0.2,
                        ),
                    weightAdaptation =
                        weightAdaptation(),
                )

        /*
         * (0.8 * 1.0 + 0.2 * 0.5)
         * * 1.0
         * * 0.20
         *
         * = 0.18
         */
        assertEquals(
            0.18,
            result.attentionDelta,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun attentionDeltaRespectsMaximumDelta() {

        val result =
            DeterministicDynamicScheduleAttentionAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 1.0,
                            recentActivity = 1.0,
                            postponement = 1.0,
                        ),
                    weightAdaptation =
                        weightAdaptation(),
                    policy =
                        DynamicScheduleAttentionAdaptationPolicy(
                            maximumDelta = 0.10,
                        ),
                )

        assertEquals(
            0.10,
            result.attentionDelta,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun completionDoesNotDirectlyIncreaseAttentionByDefault() {

        val result =
            DeterministicDynamicScheduleAttentionAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 1.0,
                            completion = 1.0,
                            postponement = 0.0,
                            recentActivity = 0.0,
                        ),
                    weightAdaptation =
                        weightAdaptation(),
                )

        assertEquals(
            0.0,
            result.attentionDelta,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun cancellationDoesNotDirectlyDecreaseAttentionByDefault() {

        val result =
            DeterministicDynamicScheduleAttentionAdapter()
                .adapt(
                    behavioral =
                        behavioral(
                            confidence = 1.0,
                            cancellation = 1.0,
                            postponement = 0.0,
                            recentActivity = 0.0,
                        ),
                    weightAdaptation =
                        weightAdaptation(),
                )

        assertEquals(
            0.0,
            result.attentionDelta,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun adaptationIsDeterministic() {

        val model =
            behavioral()

        val adapter =
            DeterministicDynamicScheduleAttentionAdapter()

        val first =
            adapter.adapt(
                behavioral = model,
                weightAdaptation =
                    weightAdaptation(),
            )

        val second =
            adapter.adapt(
                behavioral = model,
                weightAdaptation =
                    weightAdaptation(),
            )

        assertEquals(
            first,
            second,
        )
    }
}
