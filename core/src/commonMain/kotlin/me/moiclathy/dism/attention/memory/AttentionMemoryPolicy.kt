package me.moiclathy.dism.attention.memory

import kotlin.math.exp
import kotlin.math.max

/** Controls temporal decay of adaptive attention memory. */
data class AttentionMemoryPolicy(
    val halfLifeSeconds: Double = 7.0 * 24.0 * 60.0 * 60.0,
    val minimumRetention: Double = 0.05,
    val minBias: Double = -1.0,
    val maxBias: Double = 1.0,
) {
    init {
        require(halfLifeSeconds > 0.0)
        require(minimumRetention in 0.0..1.0)
        require(minBias <= maxBias)
    }

    fun retentionFactor(elapsedSeconds: Double): Double {
        if (elapsedSeconds <= 0.0) return 1.0
        val raw = exp(-elapsedSeconds * kotlin.math.ln(2.0) / halfLifeSeconds)
        return max(minimumRetention, raw)
    }
}
