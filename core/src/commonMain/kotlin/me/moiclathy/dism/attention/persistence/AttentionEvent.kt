package me.moiclathy.dism.attention.persistence

import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation

/** Append-only record of an attention lifecycle observation. */
data class AttentionEvent(
    val id: String,
    val attentionKey: String,
    val type: AttentionEventType,
    val occurredAt: Instant,
    val action: String? = null,
    val feedbackAction: AttentionFeedbackAction? = null,
    val explanation: AttentionDecisionExplanation? = null,
    val metadata: Map<String, String> = emptyMap(),
)
