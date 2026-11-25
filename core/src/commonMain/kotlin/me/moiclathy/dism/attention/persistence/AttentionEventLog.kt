package me.moiclathy.dism.attention.persistence

/**
 * Minimal local-first event log abstraction.
 * Implementations may later be backed by SQLite/SQLDelight, a file, or another local store.
 */
interface AttentionEventLog {
    fun append(event: AttentionEvent)
    fun appendAll(events: Iterable<AttentionEvent>)
    fun eventsFor(attentionKey: String): List<AttentionEvent>
    fun all(): List<AttentionEvent>
}
