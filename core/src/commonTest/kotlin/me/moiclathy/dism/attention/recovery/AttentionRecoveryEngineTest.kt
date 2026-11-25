package me.moiclathy.dism.attention.recovery

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventLog
import me.moiclathy.dism.attention.persistence.AttentionEventType

class AttentionRecoveryEngineTest {
    private val first = Instant.parse("2026-01-01T10:00:00Z")
    private val second = Instant.parse("2026-01-01T10:01:00Z")

    @Test
    fun recoveryIsChronologicalAndUsesLatestDecision() {
        val events = listOf(
            AttentionEvent("2", "a", AttentionEventType.DECISION, second, action = "ACKNOWLEDGED"),
            AttentionEvent("1", "a", AttentionEventType.DECISION, first, action = "SHOW"),
        )

        val state = AttentionRecoveryEngine().recover(events)!!
        assertEquals("ACKNOWLEDGED", state.latestAction)
        assertEquals(first, state.firstOccurredAt)
        assertEquals(second, state.lastOccurredAt)
    }

    @Test
    fun duplicateEventIdsAreAppliedOnlyOnce() {
        val event = AttentionEvent("1", "a", AttentionEventType.DECISION, first, action = "SHOW")
        val state = AttentionRecoveryEngine().recover(listOf(event, event))!!
        assertEquals(1, state.eventCount)
        assertEquals(1, state.decisionCount)
    }

    @Test
    fun feedbackIsRecoveredSeparatelyFromDecisionState() {
        val event = AttentionEvent(
            "2", "a", AttentionEventType.FEEDBACK, second,
            feedbackAction = AttentionFeedbackAction.COMPLETED,
        )
        val state = AttentionRecoveryEngine().recover(listOf(event))!!
        assertEquals(AttentionFeedbackAction.COMPLETED, state.latestFeedbackAction)
        assertEquals(0, state.decisionCount)
        assertEquals(1, state.feedbackCount)
    }

    @Test
    fun emptyRecoveryReturnsNull() {
        assertNull(AttentionRecoveryEngine().recover(emptyList()))
    }

    @Test
    fun recoverAllRebuildsIndependentAttentionKeys() {
        val log = object : AttentionEventLog {
            private val events = listOf(
                AttentionEvent("1", "a", AttentionEventType.DECISION, first, action = "SHOW"),
                AttentionEvent("2", "b", AttentionEventType.DECISION, second, action = "SUPPRESS"),
            )
            override fun append(event: AttentionEvent) = Unit
            override fun appendAll(events: Iterable<AttentionEvent>) = Unit
            override fun eventsFor(attentionKey: String) = events.filter { it.attentionKey == attentionKey }
            override fun all() = events
        }

        val states = AttentionRecoveryEngine().recoverAll(log)
        assertEquals(setOf("a", "b"), states.keys)
        assertEquals("SHOW", states.getValue("a").latestAction)
        assertEquals("SUPPRESS", states.getValue("b").latestAction)
    }
}
