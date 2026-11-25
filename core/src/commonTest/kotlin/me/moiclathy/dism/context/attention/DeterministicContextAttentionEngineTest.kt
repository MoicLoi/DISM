package me.moiclathy.dism.context.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

class DeterministicContextAttentionEngineTest {

    private val engine =
        DeterministicContextAttentionEngine()

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    @Test
    fun neutralSemanticWeightDoesNotChangePriority() {

        val target =
            ContextAttentionTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.60,
                semanticWeight = 0.50,
                contextConfidence = 1.0,
                now = now,
            )

        assertEquals(
            0.60,
            result.priority.value,
            0.000001,
        )

        assertEquals(
            0.0,
            result.priority.adjustment,
            0.000001,
        )
    }

    @Test
    fun highSemanticWeightIncreasesPriority() {

        val target =
            ContextAttentionTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.50,
                semanticWeight = 0.80,
                contextConfidence = 1.0,
                now = now,
            )

        assertEquals(
            0.74,
            result.priority.value,
            0.000001,
        )

        assertTrue(
            result.priority.adjustment > 0.0
        )
    }

    @Test
    fun lowSemanticWeightDecreasesPriority() {

        val target =
            ContextAttentionTarget(
                targetId = "sleep-reminder",
                targetType = "REMINDER",
                contextMode =
                    ContextMode.SLEEPING,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.80,
                semanticWeight = 0.20,
                contextConfidence = 1.0,
                now = now,
            )

        assertEquals(
            0.56,
            result.priority.value,
            0.000001,
        )

        assertTrue(
            result.priority.adjustment < 0.0
        )
    }

    @Test
    fun lowContextConfidenceReducesContextImpact() {

        val target =
            ContextAttentionTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.50,
                semanticWeight = 0.80,
                contextConfidence = 0.50,
                now = now,
            )

        assertEquals(
            0.62,
            result.priority.value,
            0.000001,
        )
    }

    @Test
    fun zeroContextConfidenceRemovesContextImpact() {

        val target =
            ContextAttentionTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.70,
                semanticWeight = 0.90,
                contextConfidence = 0.0,
                now = now,
            )

        assertEquals(
            0.70,
            result.priority.value,
            0.000001,
        )
    }

    @Test
    fun priorityCannotExceedOne() {

        val target =
            ContextAttentionTarget(
                targetId = "critical-event",
                targetType = "EVENT",
                contextMode =
                    ContextMode.EMERGENCY,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.95,
                semanticWeight = 1.0,
                contextConfidence = 1.0,
                now = now,
            )

        assertTrue(
            result.priority.value <= 1.0
        )
    }

    @Test
    fun priorityCannotFallBelowZero() {

        val target =
            ContextAttentionTarget(
                targetId = "ordinary-reminder",
                targetType = "REMINDER",
                contextMode =
                    ContextMode.SLEEPING,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.05,
                semanticWeight = 0.0,
                contextConfidence = 1.0,
                now = now,
            )

        assertTrue(
            result.priority.value >= 0.0
        )
    }

    @Test
    fun customPolicyCanReduceContextInfluence() {

        val target =
            ContextAttentionTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val policy =
            ContextAttentionPolicy(
                maximumAdjustment = 0.10,
            )

        val result =
            engine.evaluate(
                target = target,
                basePriority = 0.50,
                semanticWeight = 1.0,
                contextConfidence = 1.0,
                now = now,
                policy = policy,
            )

        assertEquals(
            0.60,
            result.priority.value,
            0.000001,
        )
    }

    @Test
    fun evaluationIsDeterministic() {

        val target =
            ContextAttentionTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val first =
            engine.evaluate(
                target = target,
                basePriority = 0.60,
                semanticWeight = 0.80,
                contextConfidence = 0.75,
                now = now,
            )

        val second =
            engine.evaluate(
                target = target,
                basePriority = 0.60,
                semanticWeight = 0.80,
                contextConfidence = 0.75,
                now = now,
            )

        assertEquals(
            first,
            second,
        )
    }
}
