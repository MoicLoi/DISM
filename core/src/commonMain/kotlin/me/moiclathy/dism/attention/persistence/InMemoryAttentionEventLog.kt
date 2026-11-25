package me.moiclathy.dism.attention.persistence

/** Deterministic reference implementation for core tests and prototyping. */
class InMemoryAttentionEventLog : AttentionEventLog {
    private val events = mutableListOf<AttentionEvent>()

    override fun append(event: AttentionEvent) {
        events += event
    }

    override fun appendAll(events: Iterable<AttentionEvent>) {
        this.events += events
    }

    override fun eventsFor(attentionKey: String): List<AttentionEvent> =
        events.filter { it.attentionKey == attentionKey }

    override fun all(): List<AttentionEvent> = events.toList()
}
