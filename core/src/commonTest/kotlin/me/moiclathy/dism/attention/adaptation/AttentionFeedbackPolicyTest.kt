package me.moiclathy.dism.attention.adaptation

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

class AttentionFeedbackPolicyTest {
    private val now = Instant.parse("2026-01-01T00:00:00Z")

    @Test
    fun dismissedRaisesSuppressionBias() {
        val before = AttentionAdaptation("r1")
        val after = AttentionFeedbackPolicy().apply(
            before,
            AttentionFeedback("r1", AttentionFeedbackAction.DISMISSED, now),
        )
        assertTrue(after.suppressionBias > before.suppressionBias)
        assertEquals(1, after.sampleCount)
    }

    @Test
    fun openedDoesNotSuppressTheSameAttention() {
        val after = AttentionFeedbackPolicy().apply(
            AttentionAdaptation("r1"),
            AttentionFeedback("r1", AttentionFeedbackAction.OPENED, now),
        )
        assertTrue(after.suppressionBias < 0.0)
    }

    @Test
    fun learningIsBounded() {
        var state = AttentionAdaptation("r1")
        repeat(100) {
            state = AttentionFeedbackPolicy(1.0).apply(
                state,
                AttentionFeedback("r1", AttentionFeedbackAction.DISMISSED, now),
            )
        }
        assertEquals(1.0, state.suppressionBias)
        assertTrue(state.sampleCount == 100)
    }
}
