package me.moiclathy.dism.context.e2e

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.context.attention.runtime.DeterministicContextAttentionRuntimeAdapter
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.recommendation.DeterministicContextualRecommendationAdapter
import me.moiclathy.dism.context.semantic.DeterministicContextSemanticRuntimeAdapter
import me.moiclathy.dism.context.semantics.DeterministicContextSemanticEngine

class ContextSemanticAttentionE2ERunnerTest {

    private val runner =
        ContextSemanticAttentionE2ERunner(
            semanticAdapter =
                DeterministicContextSemanticRuntimeAdapter(
                    engine =
                        DeterministicContextSemanticEngine(),
                ),

            recommendationAdapter =
                DeterministicContextualRecommendationAdapter(),

            attentionAdapter =
                DeterministicContextAttentionRuntimeAdapter(),
        )

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    @Test
    fun completePipelineShouldSucceed() {

        val result =
            runner.run(
                attentionKey =
                    "recommendation:experiment-class:bring-uniform",

                targetId =
                    "experiment-class",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_CLASS,

                basePriority =
                    0.55,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    1.0,

                evaluatedAt =
                    now,
            )

        assertTrue(
            result.contractSatisfied,
        )

        assertTrue(
            result.errors.isEmpty(),
        )

        assertEquals(
            5,
            result.trace.steps.size,
        )
    }

    @Test
    fun contextShouldPropagateIntoSemanticWeight() {

        val result =
            runner.run(
                attentionKey =
                    "recommendation:test",

                targetId =
                    "test-event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_CLASS,

                basePriority =
                    0.50,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    1.0,

                evaluatedAt =
                    now,
            )

        assertTrue(
            result.trace.contextualSemanticWeight >
                0.50,
        )
    }

    @Test
    fun semanticChangeShouldPropagateIntoRecommendation() {

        val result =
            runner.run(
                attentionKey =
                    "recommendation:test",

                targetId =
                    "test-event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_CLASS,

                basePriority =
                    0.50,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    1.0,

                evaluatedAt =
                    now,
            )

        assertTrue(
            result.trace.contextualRecommendationPriority >
                result.trace.basePriority,
        )
    }

    @Test
    fun recommendationPriorityShouldReachAttention() {

        val result =
            runner.run(
                attentionKey =
                    "recommendation:test",

                targetId =
                    "test-event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_CLASS,

                basePriority =
                    0.50,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    1.0,

                evaluatedAt =
                    now,
            )

        assertEquals(
            result.trace.contextualRecommendationPriority,
            result.trace.attentionPriority,
            0.000001,
        )
    }

    @Test
    fun sleepingContextCanSuppressSemanticWeight() {

        val result =
            runner.run(
                attentionKey =
                    "recommendation:sleep",

                targetId =
                    "reminder",

                targetType =
                    "REMINDER",

                contextMode =
                    ContextMode.SLEEPING,

                basePriority =
                    0.80,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    1.0,

                evaluatedAt =
                    now,
            )

        assertTrue(
            result.trace.contextualSemanticWeight <
                0.50,
        )

        assertTrue(
            result.trace.contextualRecommendationPriority <
                result.trace.basePriority,
        )
    }

    @Test
    fun lowContextConfidenceShouldReduceContextEffect() {

        val highConfidence =
            runner.run(
                attentionKey =
                    "recommendation:high",

                targetId =
                    "event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_CLASS,

                basePriority =
                    0.50,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    1.0,

                evaluatedAt =
                    now,
            )

        val lowConfidence =
            runner.run(
                attentionKey =
                    "recommendation:low",

                targetId =
                    "event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_CLASS,

                basePriority =
                    0.50,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    0.20,

                evaluatedAt =
                    now,
            )

        val highDelta =
            highConfidence
                .trace
                .contextualRecommendationPriority -
                highConfidence.trace.basePriority

        val lowDelta =
            lowConfidence
                .trace
                .contextualRecommendationPriority -
                lowConfidence.trace.basePriority

        assertTrue(
            highDelta > lowDelta,
        )
    }

    @Test
    fun attentionKeyShouldRemainStable() {

        val result =
            runner.run(
                attentionKey =
                    "recommendation:stable-key",

                targetId =
                    "event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_TRANSIT,

                basePriority =
                    0.50,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    0.90,

                evaluatedAt =
                    now,
            )

        assertEquals(
            "recommendation:stable-key",
            result.trace.attentionKey,
        )
    }

    @Test
    fun pipelineShouldBeDeterministic() {

        val first =
            runner.run(
                attentionKey =
                    "recommendation:deterministic",

                targetId =
                    "event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_TRANSIT,

                basePriority =
                    0.60,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    0.80,

                evaluatedAt =
                    now,
            )

        val second =
            runner.run(
                attentionKey =
                    "recommendation:deterministic",

                targetId =
                    "event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.IN_TRANSIT,

                basePriority =
                    0.60,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    0.80,

                evaluatedAt =
                    now,
            )

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun requiredPhasesShouldBePresent() {

        val result =
            runner.run(
                attentionKey =
                    "recommendation:phase-test",

                targetId =
                    "event",

                targetType =
                    "EVENT",

                contextMode =
                    ContextMode.NORMAL,

                basePriority =
                    0.50,

                baseSemanticWeight =
                    0.50,

                contextConfidence =
                    1.0,

                evaluatedAt =
                    now,
            )

        val phases =
            result.trace.steps
                .map { it.phase }

        assertTrue(
            ContextSemanticAttentionE2EPhase.CONTEXT
                in phases,
        )

        assertTrue(
            ContextSemanticAttentionE2EPhase.SEMANTIC
                in phases,
        )

        assertTrue(
            ContextSemanticAttentionE2EPhase.RECOMMENDATION
                in phases,
        )

        assertTrue(
            ContextSemanticAttentionE2EPhase.ATTENTION
                in phases,
        )

        assertTrue(
            ContextSemanticAttentionE2EPhase.FINAL
                in phases,
        )
    }
}
