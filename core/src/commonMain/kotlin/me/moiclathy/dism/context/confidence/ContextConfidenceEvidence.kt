package me.moiclathy.dism.context.confidence

import kotlin.time.Instant
import me.moiclathy.dism.context.source.ContextSourceId
import me.moiclathy.dism.context.source.ContextSourceType

/**
 * 一次影响 Context Confidence 的证据。
 *
 * 它不直接改变 Context。
 */
data class ContextConfidenceEvidence(
    val sourceId: ContextSourceId,
    val sourceType: ContextSourceType,
    val strength: Double,
    val observedAt: Instant,
    val expiresAt: Instant? = null,
    val reason: String? = null,
) {

    init {
        require(strength in 0.0..1.0) {
            "Evidence strength must be between 0.0 and 1.0."
        }
    }

    fun isExpired(
        now: Instant,
    ): Boolean {
        val expiry = expiresAt ?: return false
        return now >= expiry
    }
}
