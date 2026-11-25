package me.moiclathy.dism.attention.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionFeedback
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction

class AttentionRuntimeTest {
    @Test
    fun feedbackUpdatesAdaptationAndMemoryTogether() {
        val runtime = AttentionRuntime()
        val now = Instant.parse("2026-01-01T08:00:00Z")
        val feedback = AttentionFeedback(
            attentionKey = "attention:test",
            action = AttentionFeedbackAction.DISMISSED,
            observedAt = now,
        )

        val result = runtime.applyFeedback(AttentionRuntimeState(), feedback)

        assertEquals(1, result.adaptations["attention:test"]?.sampleCount)
        assertTrue((result.memories["attention:test"]?.bias ?: 0.0) > 0.0)
    }

    @Test
    fun repeatedFeedbackRemainsBounded() {
        val runtime = AttentionRuntime()
        val start = Instant.parse("2026-01-01T08:00:00Z")
        var state = AttentionRuntimeState()

        repeat(100) { index ->
            state = runtime.applyFeedback(
                state,
                AttentionFeedback(
                    attentionKey = "attention:test",
                    action = AttentionFeedbackAction.DISMISSED,
                    observedAt = start + index.hours,
                ),
            )
        }

        assertTrue((state.adaptations["attention:test"]?.suppressionBias ?: 0.0) <= 1.0)
        assertTrue((state.memories["attention:test"]?.bias ?: 0.0) <= 1.0)
    }
}
