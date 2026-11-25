package me.moiclathy.dism.attention.persistence.bootstrap

import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant
import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventLog
import me.moiclathy.dism.attention.recovery.AttentionRecoveryEngine

/**
 * Explicit startup boundary between persisted Attention history and the runtime.
 *
 * It performs recovery only; it never evaluates recommendations and never mutates
 * the append-only log. The caller can use the returned projection to construct the
 * initial AttentionRuntimeState before its first evaluate() call.
 */
@OptIn(ExperimentalTime::class)
class AttentionRuntimePersistenceBootstrap(
    private val eventLog: AttentionEventLog,
    private val recoveryEngine: AttentionRecoveryEngine = AttentionRecoveryEngine(),
    private val clock: Clock = Clock.System,
) {
    fun restore(): AttentionStartupState {
        val events = eventLog.all()
        return AttentionStartupState(
            recovered = recoveryEngine.recoverAll(eventLog),
            restoredAt = clock.now(),
            eventCount = events.distinctBy(AttentionEvent::id).size,
        )
    }

    /** Restore one stable attention key without changing the persisted history. */
    fun restore(attentionKey: String) = recoveryEngine.recover(attentionKey, eventLog)
}
