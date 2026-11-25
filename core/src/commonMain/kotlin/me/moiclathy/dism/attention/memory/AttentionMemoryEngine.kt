package me.moiclathy.dism.attention.memory

import kotlin.time.Instant
import kotlin.math.max
import kotlin.math.min

/** Pure, deterministic temporal decay for adaptive attention signals. */
class AttentionMemoryEngine(
    private val policy: AttentionMemoryPolicy = AttentionMemoryPolicy(),
) {
    fun decay(memory: AttentionMemory, now: Instant): AttentionMemory {
        val updated = memory.lastUpdatedAt ?: return memory
        val elapsed = max(0.0, (now - updated).inWholeNanoseconds / 1_000_000_000.0)
        if (elapsed <= 0.0) return memory.copy(bias = clamp(memory.bias))

        val retention = policy.retentionFactor(elapsed)
        return memory.copy(
            bias = clamp(memory.bias * retention),
            lastUpdatedAt = now,
        )
    }

    fun apply(
        memory: AttentionMemory,
        delta: Double,
        now: Instant,
    ): AttentionMemory {
        val decayed = decay(memory, now)
        return decayed.copy(
            bias = clamp(decayed.bias + delta),
            lastUpdatedAt = now,
        )
    }

    private fun clamp(value: Double): Double =
        min(policy.maxBias, max(policy.minBias, value))
}
