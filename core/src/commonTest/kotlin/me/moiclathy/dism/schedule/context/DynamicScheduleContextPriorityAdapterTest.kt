package me.moiclathy.dism.schedule.context

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DynamicScheduleContextPriorityAdapterTest {

    private val itemId =
        DynamicScheduleItemId("item-1")

    @Test
    fun noPreferredContextKeepsFactorAtOne() {

        val match =
            DynamicScheduleContextMatch(
                itemId = itemId,
                requiredSatisfied = true,
                excludedSatisfied = false,
                preferredScore = 0.0,
                matchedContextKeys = emptySet(),
                unmatchedRequiredContextKeys = emptySet(),
                matchedExcludedContextKeys = emptySet(),
            )

        val result =
            DeterministicDynamicScheduleContextPriorityAdapter()
                .adapt(match)

        assertEquals(
            1.0,
            result.contextFactor,
        )
    }

    @Test
    fun fullyMatchedPreferredContextRaisesPriority() {

        val match =
            DynamicScheduleContextMatch(
                itemId = itemId,
                requiredSatisfied = true,
                excludedSatisfied = false,
                preferredScore = 1.0,
                matchedContextKeys =
                    setOf("after_class"),
                unmatchedRequiredContextKeys = emptySet(),
                matchedExcludedContextKeys = emptySet(),
            )

        val result =
            DeterministicDynamicScheduleContextPriorityAdapter()
                .adapt(match)

        assertTrue(
            result.contextFactor > 1.0,
        )

        assertEquals(
            1.30,
            result.contextFactor,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun missingRequiredContextReducesPriority() {

        val match =
            DynamicScheduleContextMatch(
                itemId = itemId,
                requiredSatisfied = false,
                excludedSatisfied = false,
                preferredScore = 0.0,
                matchedContextKeys = emptySet(),
                unmatchedRequiredContextKeys =
                    setOf("after_class"),
                matchedExcludedContextKeys = emptySet(),
            )

        val result =
            DeterministicDynamicScheduleContextPriorityAdapter()
                .adapt(match)

        assertEquals(
            0.50,
            result.contextFactor,
            absoluteTolerance = 0.000001,
        )

        assertTrue(
            !result.requiredSatisfied,
        )
    }

    @Test
    fun excludedContextHasStrongestReduction() {

        val match =
            DynamicScheduleContextMatch(
                itemId = itemId,
                requiredSatisfied = true,
                excludedSatisfied = true,
                preferredScore = 1.0,
                matchedContextKeys =
                    setOf("already_done"),
                unmatchedRequiredContextKeys = emptySet(),
                matchedExcludedContextKeys =
                    setOf("already_done"),
            )

        val result =
            DeterministicDynamicScheduleContextPriorityAdapter()
                .adapt(match)

        assertEquals(
            0.25,
            result.contextFactor,
            absoluteTolerance = 0.000001,
        )

        assertTrue(
            result.excluded,
        )
    }

    @Test
    fun partialPreferredContextProducesPartialInfluence() {

        val match =
            DynamicScheduleContextMatch(
                itemId = itemId,
                requiredSatisfied = true,
                excludedSatisfied = false,
                preferredScore = 0.5,
                matchedContextKeys =
                    setOf("after_class"),
                unmatchedRequiredContextKeys = emptySet(),
                matchedExcludedContextKeys = emptySet(),
            )

        val result =
            DeterministicDynamicScheduleContextPriorityAdapter()
                .adapt(match)

        assertEquals(
            1.15,
            result.contextFactor,
            absoluteTolerance = 0.000001,
        )
    }

    @Test
    fun contextInfluenceIsDeterministic() {

        val match =
            DynamicScheduleContextMatch(
                itemId = itemId,
                requiredSatisfied = true,
                excludedSatisfied = false,
                preferredScore = 0.8,
                matchedContextKeys =
                    setOf("after_class"),
                unmatchedRequiredContextKeys = emptySet(),
                matchedExcludedContextKeys = emptySet(),
            )

        val adapter =
            DeterministicDynamicScheduleContextPriorityAdapter()

        val first =
            adapter.adapt(match)

        val second =
            adapter.adapt(match)

        assertEquals(
            first,
            second,
        )
    }
}
