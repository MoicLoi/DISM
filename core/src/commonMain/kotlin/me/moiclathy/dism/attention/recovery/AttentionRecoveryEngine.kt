package me.moiclathy.dism.attention.recovery

import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventLog
import me.moiclathy.dism.attention.persistence.AttentionEventType

/**
 * Rebuilds observable attention state without mutating the event log.
 * Event IDs are used as stable de-duplication keys during replay.
 */
class AttentionRecoveryEngine {
    fun recover(attentionKey: String, log: AttentionEventLog): AttentionRecoveryState? =
        recover(log.eventsFor(attentionKey))

    fun recover(events: Iterable<AttentionEvent>): AttentionRecoveryState? {
        val ordered = events
            .distinctBy { it.id }
            .sortedWith(compareBy<AttentionEvent> { it.occurredAt }.thenBy { it.id })

        if (ordered.isEmpty()) return null

        val key = ordered.first().attentionKey
        require(ordered.all { it.attentionKey == key }) {
            "All events in one recovery operation must belong to the same attentionKey"
        }

        var latestAction: String? = null
        var latestFeedback = ordered.firstOrNull { it.feedbackAction != null }?.feedbackAction
        var latestExplanation = ordered.firstOrNull { it.explanation != null }?.explanation
        var lastEventAt = ordered.first().occurredAt

        for (event in ordered) {
            lastEventAt = event.occurredAt
            if (event.type == AttentionEventType.DECISION && event.action != null) {
                latestAction = event.action
            }
            if (event.feedbackAction != null) latestFeedback = event.feedbackAction
            if (event.explanation != null) latestExplanation = event.explanation
        }

        return AttentionRecoveryState(
            attentionKey = key,
            latestAction = latestAction,
            latestFeedbackAction = latestFeedback,
            latestExplanation = latestExplanation,
            firstOccurredAt = ordered.first().occurredAt,
            lastOccurredAt = lastEventAt,
            eventCount = ordered.size,
            decisionCount = ordered.count { it.type == AttentionEventType.DECISION },
            feedbackCount = ordered.count { it.type == AttentionEventType.FEEDBACK },
        )
    }

    fun recoverAll(log: AttentionEventLog): Map<String, AttentionRecoveryState> =
        log.all()
            .distinctBy { it.id }
            .groupBy { it.attentionKey }
            .mapNotNull { (key, events) -> recover(events)?.let { key to it } }
            .toMap()
}
