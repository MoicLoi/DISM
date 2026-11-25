package me.moiclathy.dism.attention.persistence.bootstrap

import kotlin.time.Instant
import me.moiclathy.dism.attention.persistence.AttentionEvent
import me.moiclathy.dism.attention.persistence.AttentionEventType
import me.moiclathy.dism.attention.persistence.InMemoryAttentionEventLog
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class AttentionRuntimePersistenceBootstrapTest {
    @Test
    fun restoreReconstructsAllAttentionKeysBeforeRuntimeEvaluation() {
        val log = InMemoryAttentionEventLog()
        log.append(AttentionEvent("a1", "attention-a", AttentionEventType.DECISION, Instant.parse("2026-01-01T10:00:00Z"), action = "SHOW"))
        log.append(AttentionEvent("b1", "attention-b", AttentionEventType.DECISION, Instant.parse("2026-01-01T10:01:00Z"), action = "SUPPRESS"))
        val bootstrap = AttentionRuntimePersistenceBootstrap(log)

        val restored = bootstrap.restore()

        assertEquals(2, restored.attentionCount)
        assertEquals(2, restored.eventCount)
        assertEquals("SHOW", restored.recovered.getValue("attention-a").latestAction)
        assertEquals("SUPPRESS", restored.recovered.getValue("attention-b").latestAction)
    }

    @Test
    fun duplicateEventIdsAreCountedOnceAndRecoveryRemainsDeterministic() {
        val log = InMemoryAttentionEventLog()
        val event = AttentionEvent("same", "attention-a", AttentionEventType.DECISION, Instant.parse("2026-01-01T10:00:00Z"), action = "SHOW")
        log.append(event)
        log.append(event)
        val bootstrap = AttentionRuntimePersistenceBootstrap(log)

        val restored = bootstrap.restore()

        assertEquals(1, restored.eventCount)
        assertEquals(1, restored.recovered.getValue("attention-a").eventCount)
    }

    @Test
    fun restoreDoesNotMutateTheEventLog() {
        val log = InMemoryAttentionEventLog()
        log.append(AttentionEvent("a1", "attention-a", AttentionEventType.DECISION, Instant.parse("2026-01-01T10:00:00Z"), action = "SHOW"))
        val before = log.all()

        AttentionRuntimePersistenceBootstrap(log).restore()

        assertEquals(before, log.all())
        assertNotSame(before, log.all())
    }
}
