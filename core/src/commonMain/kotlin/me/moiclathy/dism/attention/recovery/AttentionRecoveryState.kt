package me.moiclathy.dism.attention.recovery

import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation

/** Deterministic, replayable state reconstructed from attention events. */
data class AttentionRecoveryState(
    val attentionKey: String,
    val latestAction: String? = null,
    val latestFeedbackAction: AttentionFeedbackAction? = null,
    val latestExplanation: AttentionDecisionExplanation? = null,
    val firstOccurredAt: Instant? = null,
    val lastOccurredAt: Instant? = null,
    val eventCount: Int = 0,
    val decisionCount: Int = 0,
    val feedbackCount: Int = 0,
)
