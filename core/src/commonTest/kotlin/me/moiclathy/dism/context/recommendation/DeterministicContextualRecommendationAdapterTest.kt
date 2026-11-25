package me.moiclathy.dism.context.recommendation

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.context.model.ContextMode

class DeterministicContextualRecommendationAdapterTest {

    private val adapter =
        DeterministicContextualRecommendationAdapter()

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    private fun target(
        mode: ContextMode,
    ) =
        ContextualRecommendationTarget(
            targetId =
                "experiment-class",
            targetType =
                "EVENT",
            contextMode =
                mode,
        )

    @Test
    fun relevantContextIncreasesPriority() {

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.IN_CLASS
                        ),
                    basePriority = 0.50,
                    contextualSemanticWeight = 0.80,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertTrue(
            result.adjustment.adjustedPriority >
                result.adjustment.basePriority
        )

        assertTrue(
            result.adjustment.delta > 0.0
        )
    }

    @Test
    fun irrelevantContextCanDecreasePriority() {

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.SLEEPING
                        ),
                    basePriority = 0.80,
                    contextualSemanticWeight = 0.20,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertTrue(
            result.adjustment.adjustedPriority <
                result.adjustment.basePriority
        )

        assertTrue(
            result.adjustment.delta < 0.0
        )
    }

    @Test
    fun neutralSemanticWeightDoesNotChangePriority() {

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.NORMAL
                        ),
                    basePriority = 0.70,
                    contextualSemanticWeight = 0.50,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            0.70,
            result.adjustment.adjustedPriority,
            0.000001,
        )

        assertEquals(
            0.0,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun zeroConfidencePreservesPriority() {

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.IN_CLASS
                        ),
                    basePriority = 0.70,
                    contextualSemanticWeight = 1.0,
                    contextConfidence = 0.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            0.70,
            result.adjustment.adjustedPriority,
            0.000001,
        )

        assertEquals(
            0.0,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun maximumAdjustmentIsBounded() {

        val adapter =
            DeterministicContextualRecommendationAdapter(
                maximumAdjustment = 0.10,
            )

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.IN_CLASS
                        ),
                    basePriority = 0.50,
                    contextualSemanticWeight = 1.0,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            0.60,
            result.adjustment.adjustedPriority,
            0.000001,
        )
    }

    @Test
    fun priorityIsClampedToOne() {

        val adapter =
            DeterministicContextualRecommendationAdapter(
                maximumAdjustment = 1.0,
            )

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.IN_CLASS
                        ),
                    basePriority = 0.95,
                    contextualSemanticWeight = 1.0,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            1.0,
            result.adjustment.adjustedPriority,
            0.000001,
        )
    }

    @Test
    fun priorityIsClampedToZero() {

        val adapter =
            DeterministicContextualRecommendationAdapter(
                maximumAdjustment = 1.0,
            )

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.SLEEPING
                        ),
                    basePriority = 0.05,
                    contextualSemanticWeight = 0.0,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            0.0,
            result.adjustment.adjustedPriority,
            0.000001,
        )
    }

    @Test
    fun evaluationIsDeterministic() {

        val input =
            ContextualRecommendationInput(
                target =
                    target(
                        ContextMode.IN_TRANSIT
                    ),
                basePriority = 0.60,
                contextualSemanticWeight = 0.75,
                contextConfidence = 0.80,
                evaluatedAt = now,
            )

        val first =
            adapter.evaluate(input)

        val second =
            adapter.evaluate(input)

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun evaluationTimeIsPreserved() {

        val result =
            adapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        target(
                            ContextMode.IN_CLASS
                        ),
                    basePriority = 0.50,
                    contextualSemanticWeight = 0.80,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            now,
            result.evaluatedAt,
        )
    }
}
