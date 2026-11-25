package me.moiclathy.dism.attention.recovery

import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionAdaptation
import me.moiclathy.dism.attention.adaptation.AttentionAdaptationEngine
import me.moiclathy.dism.attention.adaptation.AttentionFeedback
import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventType
import me.moiclathy.dism.attention.memory.AttentionMemory
import me.moiclathy.dism.attention.memory.AttentionMemoryEngine

/**
 * Pure event reducer. The same ordered event stream always produces the same state.
 * It does not mutate the source log and ignores duplicate event ids.
 */
class AttentionStateReducer(
    private val adaptationEngine: AttentionAdaptationEngine = AttentionAdaptationEngine(),
    private val memoryEngine: AttentionMemoryEngine = AttentionMemoryEngine(),
) {
    fun reduce(events: Iterable<AttentionEvent>): AttentionReplayState? {
        val ordered = events.distinctBy { it.id }
            .sortedWith(compareBy<AttentionEvent> { it.occurredAt }.thenBy { it.id })
        if (ordered.isEmpty()) return null

        val key = ordered.first().attentionKey
        require(ordered.all { it.attentionKey == key }) {
            "All events in one reduction must belong to the same attentionKey"
        }

        var state = AttentionReplayState(
            attentionKey = key,
            adaptation = AttentionAdaptation(key),
            firstOccurredAt = ordered.first().occurredAt,
            lastOccurredAt = ordered.first().occurredAt,
        )

        for (event in ordered) {
            state = reduceOne(state, event)
        }
        return state
    }

    private fun reduceOne(state: AttentionReplayState, event: AttentionEvent): AttentionReplayState {
        var adaptation = state.adaptation
        var memory = state.memory
        var latestAction = state.latestAction
        var latestFeedback = state.latestFeedbackAction
        var latestExplanation = state.latestExplanation

        if (event.type == AttentionEventType.DECISION && event.action != null) {
            latestAction = event.action
        }
        if (event.feedbackAction != null) {
            latestFeedback = event.feedbackAction
            val feedback = AttentionFeedback(
                attentionKey = state.attentionKey,
                action = event.feedbackAction,
                consequenceWeight = event.metadata["consequenceWeight"]?.toDoubleOrNull() ?: 1.0,
                observedAt = event.occurredAt,
            )
            val nextAdaptation = adaptationEngine.learn(adaptation, feedback)
            val suppressionDelta = nextAdaptation.suppressionBias - adaptation.suppressionBias
            adaptation = nextAdaptation
            memory = memoryEngine.apply(memory, suppressionDelta, event.occurredAt)
        }
        if (event.explanation != null) {
            latestExplanation = event.explanation
        }

        return state.copy(
            latestAction = latestAction,
            latestFeedbackAction = latestFeedback,
            latestExplanation = latestExplanation,
            adaptation = adaptation,
            memory = memory,
            lastOccurredAt = event.occurredAt,
            eventCount = state.eventCount + 1,
            decisionCount = state.decisionCount + if (event.type == AttentionEventType.DECISION) 1 else 0,
            feedbackCount = state.feedbackCount + if (event.type == AttentionEventType.FEEDBACK) 1 else 0,
        )
    }
}
