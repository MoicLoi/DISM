package me.moiclathy.dism.attention.adaptation

import kotlin.time.Instant

data class AttentionFeedback(
    val attentionKey: String,
    val action: AttentionFeedbackAction,
    val observedAt: Instant,
    val recommendationType: String? = null,
    val contextMode: String? = null,
    val consequenceWeight: Double = 1.0,
)
