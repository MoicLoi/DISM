package me.moiclathy.dism.attention.lifecycle

import kotlin.time.Duration
import kotlin.time.Instant
import kotlinx.serialization.Serializable

/**
 * Minimal local delivery memory. This is not the semantic truth itself; it is
 * only the system's memory of how much attention it has already consumed.
 */
@Serializable
data class AttentionRecord(
    val key: AttentionKey,
    val lifecycle: AttentionLifecycle = AttentionLifecycle.NEW,
    val firstSeenAt: Instant? = null,
    val lastShownAt: Instant? = null,
    val shownCount: Int = 0,
    val acknowledgedAt: Instant? = null,
    val snoozedUntil: Instant? = null,
    val lastMessageFingerprint: String? = null,
    val repeatAfter: Duration? = null,
) {
    init {
        require(shownCount >= 0)
    }
}
