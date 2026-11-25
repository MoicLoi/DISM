package me.moiclathy.dism.context.semantic

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.semantics.ContextSemanticEngine
import me.moiclathy.dism.context.semantics.ContextSemanticPolicy
import me.moiclathy.dism.context.semantics.DeterministicContextSemanticEngine

class DeterministicContextSemanticRuntimeAdapterTest {

    private val engine:
        ContextSemanticEngine =
        DeterministicContextSemanticEngine()

    private val policy =
        ContextSemanticPolicy()

    private val adapter =
        DeterministicContextSemanticRuntimeAdapter(
            engine = engine,
            policy = policy,
        )

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    @Test
    fun contextCanIncreaseSemanticWeight() {

        val target =
            me.moiclathy.dism.context.semantics
                .ContextSemanticTarget(
                    targetId = "experiment-class",
                    targetType = "EVENT",
                    contextMode =
                        ContextMode.IN_CLASS,
                )

        val result =
            adapter.evaluate(
                ContextSemanticRuntimeInput(
                    target = target,
                    baseWeight = 0.50,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertTrue(
            result.adjustment.adjustedWeight >
                result.adjustment.baseWeight,
        )

        assertTrue(
            result.adjustment.delta > 0.0,
        )
    }

    @Test
    fun zeroContextConfidencePreservesBaseWeight() {

        val target =
            me.moiclathy.dism.context.semantics
                .ContextSemanticTarget(
                    targetId = "experiment-class",
                    targetType = "EVENT",
                    contextMode =
                        ContextMode.IN_CLASS,
                )

        val result =
            adapter.evaluate(
                ContextSemanticRuntimeInput(
                    target = target,
                    baseWeight = 0.50,
                    contextConfidence = 0.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            0.50,
            result.adjustment.adjustedWeight,
            0.000001,
        )

        assertEquals(
            0.0,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun sleepingCanSuppressReminderSemanticWeight() {

        val target =
            me.moiclathy.dism.context.semantics
                .ContextSemanticTarget(
                    targetId = "reminder-1",
                    targetType = "REMINDER",
                    contextMode =
                        ContextMode.SLEEPING,
                )

        val result =
            adapter.evaluate(
                ContextSemanticRuntimeInput(
                    target = target,
                    baseWeight = 0.80,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertTrue(
            result.adjustment.adjustedWeight <
                result.adjustment.baseWeight,
        )
    }

    @Test
    fun runtimeResultKeepsEvaluation() {

        val target =
            me.moiclathy.dism.context.semantics
                .ContextSemanticTarget(
                    targetId = "experiment-class",
                    targetType = "EVENT",
                    contextMode =
                        ContextMode.IN_CLASS,
                )

        val result =
            adapter.evaluate(
                ContextSemanticRuntimeInput(
                    target = target,
                    baseWeight = 0.50,
                    contextConfidence = 1.0,
                    evaluatedAt = now,
                ),
            )

        assertEquals(
            result.evaluation.score.effectiveWeight,
            result.adjustment.adjustedWeight,
            0.000001,
        )
    }

    @Test
    fun evaluationIsDeterministic() {

        val target =
            me.moiclathy.dism.context.semantics
                .ContextSemanticTarget(
                    targetId = "experiment-class",
                    targetType = "EVENT",
                    contextMode =
                        ContextMode.IN_CLASS,
                )

        val input =
            ContextSemanticRuntimeInput(
                target = target,
                baseWeight = 0.50,
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
    fun runtimePreservesEvaluationTime() {

        val target =
            me.moiclathy.dism.context.semantics
                .ContextSemanticTarget(
                    targetId = "experiment-class",
                    targetType = "EVENT",
                    contextMode =
                        ContextMode.IN_CLASS,
                )

        val result =
            adapter.evaluate(
                ContextSemanticRuntimeInput(
                    target = target,
                    baseWeight = 0.50,
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
