package me.moiclathy.dism.schedule.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.days
import kotlin.time.Instant

class DeterministicDynamicScheduleAttentionDecayEngineTest {

    private val engine =
        DeterministicDynamicScheduleAttentionDecayEngine()

    private val now =
        Instant.parse("2026-09-21T00:00:00Z")

    @Test
    fun recentHistoryKeepsMostOfItsWeight() {

        val history =
            DynamicScheduleAttentionHistory(
                attentionKey = "schedule:item-1",
                shownCount = 10,
                ignoredCount = 4,
                lastInteractionAt =
                    Instant.parse(
                        "2026-09-20T00:00:00Z",
                    ),
            )

        val result =
            engine.decay(
                DynamicScheduleAttentionDecayInput(
                    histories =
                        mapOf(
                            history.attentionKey to history,
                        ),
                    now = now,
                ),
            )

        val decayed =
            result.histories
                .getValue(history.attentionKey)

        assertTrue(
            decayed.ignoredWeight > 3.0,
        )
    }

    @Test
    fun halfLifeReducesWeightToApproximatelyHalf() {

        val history =
            DynamicScheduleAttentionHistory(
                attentionKey = "schedule:item-1",
                shownCount = 10,
                ignoredCount = 10,
                lastInteractionAt =
                    Instant.parse(
                        "2026-08-22T00:00:00Z",
                    ),
            )

        val result =
            engine.decay(
                input =
                    DynamicScheduleAttentionDecayInput(
                        histories =
                            mapOf(
                                history.attentionKey to history,
                            ),
                        now = now,
                    ),
                policy =
                    DynamicScheduleAttentionDecayPolicy(
                        halfLife = 30.days,
                        minimumWeight = 0.0,
                    ),
            )

        val decayed =
            result.histories
                .getValue(history.attentionKey)

        assertEquals(
            5.0,
            decayed.ignoredWeight,
            0.15,
        )
    }

    @Test
    fun oldHistoryDoesNotDisappearCompletely() {

        val history =
            DynamicScheduleAttentionHistory(
                attentionKey = "schedule:item-1",
                ignoredCount = 10,
                lastInteractionAt =
                    Instant.parse(
                        "2025-01-01T00:00:00Z",
                    ),
            )

        val result =
            engine.decay(
                DynamicScheduleAttentionDecayInput(
                    histories =
                        mapOf(
                            history.attentionKey to history,
                        ),
                    now = now,
                ),
            )

        val decayed =
            result.histories
                .getValue(history.attentionKey)

        assertTrue(
            decayed.ignoredWeight > 0.0,
        )
    }

    @Test
    fun missingInteractionTimeUsesMinimumWeight() {

        val history =
            DynamicScheduleAttentionHistory(
                attentionKey = "schedule:item-1",
                ignoredCount = 10,
                lastInteractionAt = null,
            )

        val result =
            engine.decay(
                DynamicScheduleAttentionDecayInput(
                    histories =
                        mapOf(
                            history.attentionKey to history,
                        ),
                    now = now,
                ),
            )

        val decayed =
            result.histories
                .getValue(history.attentionKey)

        assertEquals(
            0.5,
            decayed.ignoredWeight,
            0.0001,
        )
    }
}
