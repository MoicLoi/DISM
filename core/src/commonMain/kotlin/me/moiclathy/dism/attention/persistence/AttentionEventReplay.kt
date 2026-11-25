package me.moiclathy.dism.attention.persistence

/**
 * Reconstructs the latest observable attention state from an append-only log.
 * Unknown/non-state metadata is intentionally preserved in the event log itself.
 */
class AttentionEventReplay {
    fun latestFor(attentionKey: String, log: AttentionEventLog): AttentionEvent? =
        log.eventsFor(attentionKey)
            .maxByOrNull { it.occurredAt }

    fun timelineFor(attentionKey: String, log: AttentionEventLog): List<AttentionEvent> =
        log.eventsFor(attentionKey).sortedBy { it.occurredAt }
}
