package me.moiclathy.dism.attention.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventType

class AttentionStateReducerTest {
    private val t1 = Instant.parse("2026-01-01T10:00:00Z")
    private val t2 = Instant.parse("2026-01-01T10:01:00Z")

    @Test
    fun feedbackIsProjectedIntoAdaptationAndMemory() {
        val events = listOf(
            AttentionEvent("1", "a", AttentionEventType.FEEDBACK, t1, feedbackAction = AttentionFeedbackAction.COMPLETED),
            AttentionEvent("2", "a", AttentionEventType.FEEDBACK, t2, feedbackAction = AttentionFeedbackAction.DISMISSED),
        )
        val state = AttentionStateReducer().reduce(events)!!
        assertEquals(2, state.feedbackCount)
        assertEquals(AttentionFeedbackAction.DISMISSED, state.latestFeedbackAction)
        assertEquals(2, state.adaptation.sampleCount)
        assertTrue(state.memory.lastUpdatedAt == t2)
    }

    @Test
    fun replayIsDeterministicAndDuplicateSafe() {
        val event = AttentionEvent("1", "a", AttentionEventType.FEEDBACK, t1, feedbackAction = AttentionFeedbackAction.OPENED)
        val first = AttentionStateReducer().reduce(listOf(event, event))!!
        val second = AttentionStateReducer().reduce(listOf(event))!!
        assertEquals(second, first)
        assertEquals(1, first.eventCount)
    }

    @Test
    fun chronologicalOrderWinsEvenWhenInputIsReversed() {
        val events = listOf(
            AttentionEvent("2", "a", AttentionEventType.DECISION, t2, action = "ACKNOWLEDGED"),
            AttentionEvent("1", "a", AttentionEventType.DECISION, t1, action = "SHOW"),
        )
        val state = AttentionStateReducer().reduce(events)!!
        assertEquals("ACKNOWLEDGED", state.latestAction)
        assertEquals(t1, state.firstOccurredAt)
        assertEquals(t2, state.lastOccurredAt)
    }
}
