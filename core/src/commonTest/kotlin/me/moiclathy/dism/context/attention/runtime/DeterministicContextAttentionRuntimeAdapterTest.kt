package me.moiclathy.dism.context.attention.runtime

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.context.model.ContextMode

class DeterministicContextAttentionRuntimeAdapterTest {

    private val adapter =
        DeterministicContextAttentionRuntimeAdapter()

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    private fun input(
        basePriority: Double,
        contextualPriority: Double,
        confidence: Double = 1.0,
        mode: ContextMode =
            ContextMode.IN_CLASS,
    ) =
        ContextAttentionRuntimeInput(
            attentionKey =
                "recommendation:experiment-class:bring-uniform",

            basePriority =
                basePriority,

            contextualPriority =
                contextualPriority,

            contextMode =
                mode,

            contextConfidence =
                confidence,

            evaluatedAt =
                now,
        )

    @Test
    fun contextualPriorityCanIncreaseAttentionPriority() {

        val result =
            adapter.evaluate(
                input(
                    basePriority = 0.55,
                    contextualPriority = 0.70,
                ),
            )

        assertTrue(
            result.adjustment.contextualPriority >
                result.adjustment.basePriority,
        )

        assertEquals(
            0.15,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun contextualPriorityCanDecreaseAttentionPriority() {

        val result =
            adapter.evaluate(
                input(
                    basePriority = 0.80,
                    contextualPriority = 0.40,
                    mode =
                        ContextMode.SLEEPING,
                ),
            )

        assertTrue(
            result.adjustment.contextualPriority <
                result.adjustment.basePriority,
        )

        assertEquals(
            -0.40,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun unchangedPriorityProducesZeroDelta() {

        val result =
            adapter.evaluate(
                input(
                    basePriority = 0.60,
                    contextualPriority = 0.60,
                ),
            )

        assertEquals(
            0.0,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun priorityIsClampedToOne() {

        val result =
            adapter.evaluate(
                input(
                    basePriority = 0.90,
                    contextualPriority = 1.0,
                ),
            )

        assertEquals(
            1.0,
            result.adjustment.contextualPriority,
            0.000001,
        )

        assertEquals(
            0.10,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun priorityIsClampedToZero() {

        val result =
            adapter.evaluate(
                input(
                    basePriority = 0.20,
                    contextualPriority = 0.0,
                ),
            )

        assertEquals(
            0.0,
            result.adjustment.contextualPriority,
            0.000001,
        )

        assertEquals(
            -0.20,
            result.adjustment.delta,
            0.000001,
        )
    }

    @Test
    fun contextConfidenceIsPreserved() {

        val result =
            adapter.evaluate(
                input(
                    basePriority = 0.50,
                    contextualPriority = 0.75,
                    confidence = 0.65,
                ),
            )

        assertEquals(
            0.65,
            result.adjustment.contextConfidence,
            0.000001,
        )
    }

    @Test
    fun contextModeIsPreserved() {

        val result =
            adapter.evaluate(
                input(
                    basePriority = 0.50,
                    contextualPriority = 0.75,
                    mode =
                        ContextMode.IN_TRANSIT,
                ),
            )

        assertEquals(
            ContextMode.IN_TRANSIT,
            result.adjustment.contextMode,
        )
    }

    @Test
    fun attentionKeyIsInputIdentityOnly() {

        val input =
            input(
                basePriority = 0.50,
                contextualPriority = 0.75,
            )

        assertEquals(
            "recommendation:experiment-class:bring-uniform",
            input.attentionKey,
        )
    }

    @Test
    fun evaluationIsDeterministic() {

        val input =
            input(
                basePriority = 0.50,
                contextualPriority = 0.75,
                confidence = 0.80,
                mode =
                    ContextMode.IN_TRANSIT,
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
                input(
                    basePriority = 0.50,
                    contextualPriority = 0.75,
                ),
            )

        assertEquals(
            now,
            result.evaluatedAt,
        )
    }
}
