package me.moiclathy.dism.attention.memory

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlin.time.Duration.Companion.days

class AttentionMemoryTest {
    private val engine = AttentionMemoryEngine(
        AttentionMemoryPolicy(halfLifeSeconds = 7.days.inWholeSeconds.toDouble())
    )

    @Test
    fun halfLifeReducesBiasByHalf() {
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val after = start + 7.days
        val result = engine.decay(AttentionMemory(1.0, start), after)
        assertEquals(0.5, result.bias, absoluteTolerance = 0.0001)
    }

    @Test
    fun memoryNeverExceedsBounds() {
        val now = Instant.parse("2026-01-01T00:00:00Z")
        val result = engine.apply(AttentionMemory(0.95, now), 0.5, now)
        assertTrue(result.bias <= 1.0)
    }

    @Test
    fun decayStopsAtMinimumRetention() {
        val policy = AttentionMemoryPolicy(
            halfLifeSeconds = 1.0,
            minimumRetention = 0.1,
        )
        val engine = AttentionMemoryEngine(policy)
        val start = Instant.parse("2026-01-01T00:00:00Z")
        val result = engine.decay(AttentionMemory(1.0, start), start + 365.days)
        assertEquals(0.1, result.bias, absoluteTolerance = 0.0001)
    }
}
