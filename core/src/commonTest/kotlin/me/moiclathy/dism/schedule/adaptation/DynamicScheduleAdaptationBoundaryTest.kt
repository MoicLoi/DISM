package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleAdaptationBoundaryTest {

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val now =
        Instant.fromEpochMilliseconds(
            1_000_000L,
        )

    private fun adaptation(
        factor: Double = 1.30,
    ) =
        DynamicSchedulePriorityAdaptation(
            itemId = itemId,
            basePriority = 0.5,
            adaptationFactor = factor,
            effectivePriority =
                (0.5 * factor)
                    .coerceIn(0.0, 1.0),
        )

    private fun confidence(
        value: Double = 0.9,
    ) =
        DynamicScheduleAdaptationConfidence(
            itemId = itemId,
            value = value,
            historicalEvidence = value,
            contextEvidence = value,
            stabilityEvidence = value,
            sampleEvidence = value,
            enabled = value >= 0.5,
        )

    @Test
    fun singleChangeBoundaryLimitsLargeAdaptation() {

        val result =
            DeterministicDynamicScheduleAdaptationBoundaryAdapter()
                .apply(
                    adaptation =
                        adaptation(1.30),

                    confidence =
                        confidence(0.9),

                    state =
                        DynamicScheduleAdaptationBoundaryState(
                            itemId = itemId,
                        ),

                    now = now,
                )

        assertEquals(
            0.10,
            result.allowedDelta,
            absoluteTolerance = 0.000001,
        )

        assertTrue(
            result.limitedBySingleChange,
        )
    }

    @Test
    fun smallAdaptationPassesThrough() {

        val result =
            DeterministicDynamicScheduleAdaptationBoundaryAdapter()
                .apply(
                    adaptation =
                        adaptation(1.05),

                    confidence =
                        confidence(0.9),

                    state =
                        DynamicScheduleAdaptationBoundaryState(
                            itemId = itemId,
                        ),

                    now = now,
                )

        assertEquals(
            0.05,
            result.allowedDelta,
            absoluteTolerance = 0.000001,
        )

        assertFalse(
            result.limitedBySingleChange,
        )
    }

    @Test
    fun cumulativeBoundaryLimitsFurtherGrowth() {

        val result =
            DeterministicDynamicScheduleAdaptationBoundaryAdapter()
                .apply(
                    adaptation =
                        adaptation(1.10),

                    confidence =
                        confidence(0.9),

                    state =
                        DynamicScheduleAdaptationBoundaryState(
                            itemId = itemId,
                            cumulativeDelta = 0.18,
                        ),

                    now = now,
                )

        assertEquals(
            0.02,
            result.allowedDelta,
            absoluteTolerance = 0.000001,
        )

        assertTrue(
            result.limitedByCumulativeChange,
        )
    }

    @Test
    fun frequencyBoundaryStopsFurtherAutomaticChanges() {

        val recent =
            listOf(
                Instant.fromEpochMilliseconds(
                    999_900L,
                ),
                Instant.fromEpochMilliseconds(
                    999_800L,
                ),
                Instant.fromEpochMilliseconds(
                    999_700L,
                ),
            )

        val result =
            DeterministicDynamicScheduleAdaptationBoundaryAdapter()
                .apply(
                    adaptation =
                        adaptation(1.05),

                    confidence =
                        confidence(0.9),

                    state =
                        DynamicScheduleAdaptationBoundaryState(
                            itemId = itemId,
                            recentChangeTimes = recent,
                        ),

                    now = now,

                    policy =
                        DynamicScheduleAdaptationBoundaryPolicy(
                            maximumChangesPerWindow = 3,
                        ),
                )

        assertEquals(
            0.0,
            result.allowedDelta,
        )

        assertTrue(
            result.limitedByFrequency,
        )

        assertFalse(
            result.enabled,
        )
    }

    @Test
    fun explicitUserSettingBlocksAutomaticAdaptation() {

        val result =
            DeterministicDynamicScheduleAdaptationBoundaryAdapter()
                .apply(
                    adaptation =
                        adaptation(1.10),

                    confidence =
                        confidence(1.0),

                    state =
                        DynamicScheduleAdaptationBoundaryState(
                            itemId = itemId,
                            explicitUserSetting = true,
                            explicitUserSettingAt = now,
                        ),

                    now = now,
                )

        assertEquals(
            0.0,
            result.allowedDelta,
        )

        assertTrue(
            result.limitedByExplicitUserSetting,
        )

        assertFalse(
            result.enabled,
        )
    }

    @Test
    fun lowConfidenceBlocksAdaptation() {

        val result =
            DeterministicDynamicScheduleAdaptationBoundaryAdapter()
                .apply(
                    adaptation =
                        adaptation(1.10),

                    confidence =
                        confidence(0.2),

                    state =
                        DynamicScheduleAdaptationBoundaryState(
                            itemId = itemId,
                        ),

                    now = now,
                )

        assertEquals(
            0.0,
            result.allowedDelta,
        )

        assertFalse(
            result.enabled,
        )
    }

    @Test
    fun boundedAdaptationConvertsDeltaBackToFactor() {

        val boundary =
            DynamicScheduleAdaptationBoundary(
                itemId = itemId,
                requestedDelta = 0.30,
                allowedDelta = 0.10,
                confidence = 0.9,
                limitedBySingleChange = true,
                limitedByCumulativeChange = false,
                limitedByFrequency = false,
                limitedByExplicitUserSetting = false,
                enabled = true,
            )

        val result =
            DynamicScheduleBoundedAdaptation
                .from(boundary)

        assertEquals(
            1.10,
            result.adaptationFactor,
            absoluteTolerance = 0.000001,
        )
    }
}
