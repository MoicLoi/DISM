package me.moiclathy.dism.attention.lifecycle

/**
 * Pure result of attention reconciliation. A decision says what the delivery
 * layer should do; it does not actually send a notification.
 */
data class AttentionDecision(
    val key: AttentionKey,
    val action: AttentionAction,
    val reason: String,
    val record: AttentionRecord,
)
