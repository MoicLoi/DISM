package me.moiclathy.dism.context.model

import kotlin.time.Instant

/** Immutable observation about the user's current situation. */
data class ContextClaim(
    val id: String,
    val contextId: ContextId,
    val mode: ContextMode,
    val confidence: Double = 1.0,
    val source: ContextSource = ContextSource.USER,
    val observedAt: Instant,
    val expiresAt: Instant? = null,
    val evidence: List<String> = emptyList(),
) {
    init {
        require(id.isNotBlank()) { "ContextClaim.id must not be blank" }
        require(confidence in 0.0..1.0) { "confidence must be in [0,1]" }
        require(expiresAt == null || expiresAt >= observedAt) { "expiresAt must not precede observedAt" }
    }

    fun isActiveAt(now: Instant): Boolean =
        now >= observedAt && (expiresAt == null || now < expiresAt)
}
