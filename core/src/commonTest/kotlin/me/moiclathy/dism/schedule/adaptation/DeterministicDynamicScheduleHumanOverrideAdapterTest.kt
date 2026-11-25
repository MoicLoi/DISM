package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DeterministicDynamicScheduleHumanOverrideAdapterTest {

    private val adapter =
        DeterministicDynamicScheduleHumanOverrideAdapter()

    private val itemId =
        DynamicScheduleItemId("item-1")

    private val adaptation =
        DynamicSchedulePriorityAdaptation(
            itemId = itemId,
            basePriority = 0.4,
            adaptationFactor = 1.25,
            effectivePriority = 0.5,
            reasons = listOf(
                "Automatic adaptation",
            ),
        )

    @Test
    fun noOverrideKeepsAutomaticResult() {

        val result =
            adapter.apply(
                DynamicScheduleHumanOverrideInput(
                    adaptations =
                        listOf(adaptation),
                ),
            )

        assertEquals(
            0.5,
            result.adaptations.single().effectivePriority,
        )

        assertTrue(
            result.overriddenItemIds.isEmpty(),
        )
    }

    @Test
    fun humanPriorityOverridesAutomaticPriority() {

        val result =
            adapter.apply(
                DynamicScheduleHumanOverrideInput(
                    adaptations =
                        listOf(adaptation),

                    overrides =
                        mapOf(
                            itemId to
                                DynamicScheduleHumanOverride(
                                    itemId = itemId,
                                    priorityOverride = 0.9,
                                    createdAt =
                                        Instant.parse(
                                            "2026-01-01T00:00:00Z",
                                        ),
                                    reason =
                                        "User explicitly marked important",
                                ),
                        ),
                ),
            )

        assertEquals(
            0.9,
            result.adaptations.single().effectivePriority,
        )

        assertTrue(
            itemId in result.overriddenItemIds,
        )
    }

    @Test
    fun humanOverrideDoesNotChangeAdaptationFactor() {

        val result =
            adapter.apply(
                DynamicScheduleHumanOverrideInput(
                    adaptations =
                        listOf(adaptation),

                    overrides =
                        mapOf(
                            itemId to
                                DynamicScheduleHumanOverride(
                                    itemId = itemId,
                                    priorityOverride = 0.9,
                                    createdAt =
                                        Instant.parse(
                                            "2026-01-01T00:00:00Z",
                                        ),
                                ),
                        ),
                ),
            )

        val resultItem =
            result.adaptations.single()

        assertEquals(
            1.25,
            resultItem.adaptationFactor,
        )

        assertEquals(
            0.9,
            resultItem.effectivePriority,
        )
    }

    @Test
    fun inactiveOverrideIsIgnored() {

        val result =
            adapter.apply(
                DynamicScheduleHumanOverrideInput(
                    adaptations =
                        listOf(adaptation),

                    overrides =
                        mapOf(
                            itemId to
                                DynamicScheduleHumanOverride(
                                    itemId = itemId,
                                    priorityOverride = 0.9,
                                    locked = false,
                                    createdAt =
                                        Instant.parse(
                                            "2026-01-01T00:00:00Z",
                                        ),
                                ),
                        ),
                ),
            )

        assertEquals(
            0.5,
            result.adaptations.single().effectivePriority,
        )

        assertTrue(
            result.overriddenItemIds.isEmpty(),
        )
    }

    @Test
    fun policyCanDisablePriorityOverride() {

        val result =
            adapter.apply(
                input =
                    DynamicScheduleHumanOverrideInput(
                        adaptations =
                            listOf(adaptation),

                        overrides =
                            mapOf(
                                itemId to
                                    DynamicScheduleHumanOverride(
                                        itemId = itemId,
                                        priorityOverride = 0.9,
                                        createdAt =
                                            Instant.parse(
                                                "2026-01-01T00:00:00Z",
                                            ),
                                    ),
                            ),
                    ),

                policy =
                    DynamicScheduleHumanOverridePolicy(
                        allowPriorityOverride = false,
                    ),
            )

        assertEquals(
            0.5,
            result.adaptations.single().effectivePriority,
        )
    }

    @Test
    fun priorityOverrideIsClampedToValidRange() {

        val result =
            adapter.apply(
                DynamicScheduleHumanOverrideInput(
                    adaptations =
                        listOf(adaptation),

                    overrides =
                        mapOf(
                            itemId to
                                DynamicScheduleHumanOverride(
                                    itemId = itemId,
                                    priorityOverride = 1.0,
                                    createdAt =
                                        Instant.parse(
                                            "2026-01-01T00:00:00Z",
                                        ),
                                ),
                        ),
                ),
            )

        assertEquals(
            1.0,
            result.adaptations.single().effectivePriority,
        )
    }
}
