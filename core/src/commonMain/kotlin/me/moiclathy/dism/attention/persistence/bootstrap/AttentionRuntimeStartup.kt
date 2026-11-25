package me.moiclathy.dism.attention.persistence.bootstrap

import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventLog

/** Small application-facing facade that makes startup ordering explicit. */
class AttentionRuntimeStartup(
    private val bootstrap: AttentionRuntimePersistenceBootstrap,
    private val eventLog: AttentionEventLog,
) {
    fun restore(): AttentionStartupState = bootstrap.restore()

    /** Persist events emitted by the first/next runtime cycle after recovery. */
    fun appendRuntimeEvents(events: Iterable<AttentionEvent>) {
        eventLog.appendAll(events)
    }
}
