package me.moiclathy.dism.context.semantics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

class DeterministicContextSemanticEngineTest {

    private val engine =
        DeterministicContextSemanticEngine()

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    @Test
    fun inClassMakesRelatedEventMoreRelevant() {

        val target =
            ContextSemanticTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val result =
            engine.evaluate(
                target = target,
                baseWeight = 0.50,
                contextConfidence = 1.0,
                now = now,
            )

        assertEquals(
            0.80,
            result.score.effectiveWeight,
            0.000001,
        )

        assertEquals(
            ContextSemanticRelation.RELEVANT,
            result.score.relation,
        )
    }

    @Test
    fun lowContextConfidenceReducesSemanticImpact() {

        val target =
            ContextSemanticTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val result =
            engine.evaluate(
                target = target,
                baseWeight = 0.50,
                contextConfidence = 0.50,
                now = now,
            )

        assertEquals(
            0.65,
            result.score.effectiveWeight,
            0.000001,
        )
    }

    @Test
    fun sleepingSuppressesReminder() {

        val target =
            ContextSemanticTarget(
                targetId = "breakfast-reminder",
                targetType = "REMINDER",
                contextMode =
                    ContextMode.SLEEPING,
            )

        val result =
            engine.evaluate(
                target = target,
                baseWeight = 0.80,
                contextConfidence = 1.0,
                now = now,
            )

        assertEquals(
            0.20,
            result.score.effectiveWeight,
            0.000001,
        )

        assertEquals(
            ContextSemanticRelation.SUPPRESSING,
            result.score.relation,
        )
    }

    @Test
    fun noRuleKeepsBaseWeight() {

        val target =
            ContextSemanticTarget(
                targetId = "random-item",
                targetType = "UNKNOWN",
                contextMode =
                    ContextMode.RESTING,
            )

        val result =
            engine.evaluate(
                target = target,
                baseWeight = 0.60,
                contextConfidence = 1.0,
                now = now,
            )

        assertEquals(
            0.60,
            result.score.effectiveWeight,
            0.000001,
        )

        assertEquals(
            ContextSemanticRelation.NEUTRAL,
            result.score.relation,
        )
    }

    @Test
    fun zeroConfidenceRemovesContextInfluence() {

        val target =
            ContextSemanticTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val result =
            engine.evaluate(
                target = target,
                baseWeight = 0.50,
                contextConfidence = 0.0,
                now = now,
            )

        assertEquals(
            0.50,
            result.score.effectiveWeight,
            0.000001,
        )
    }

    @Test
    fun weightNeverExceedsOne() {

        val target =
            ContextSemanticTarget(
                targetId = "emergency-event",
                targetType = "EVENT",
                contextMode =
                    ContextMode.EMERGENCY,
            )

        val result =
            engine.evaluate(
                target = target,
                baseWeight = 0.90,
                contextConfidence = 1.0,
                now = now,
            )

        assertTrue(
            result.score.effectiveWeight <= 1.0
        )
    }

    @Test
    fun weightNeverFallsBelowZero() {

        val target =
            ContextSemanticTarget(
                targetId = "sleep-reminder",
                targetType = "REMINDER",
                contextMode =
                    ContextMode.SLEEPING,
            )

        val result =
            engine.evaluate(
                target = target,
                baseWeight = 0.10,
                contextConfidence = 1.0,
                now = now,
            )

        assertTrue(
            result.score.effectiveWeight >= 0.0
        )
    }

    @Test
    fun evaluationIsDeterministic() {

        val target =
            ContextSemanticTarget(
                targetId = "experiment-class",
                targetType = "EVENT",
                contextMode =
                    ContextMode.IN_CLASS,
            )

        val first =
            engine.evaluate(
                target = target,
                baseWeight = 0.50,
                contextConfidence = 0.80,
                now = now,
            )

        val second =
            engine.evaluate(
                target = target,
                baseWeight = 0.50,
                contextConfidence = 0.80,
                now = now,
            )

        assertEquals(
            first,
            second,
        )
    }
}
