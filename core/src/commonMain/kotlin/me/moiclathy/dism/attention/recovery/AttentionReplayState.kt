package me.moiclathy.dism.attention.recovery

import kotlin.time.Instant
import me.moiclathy.dism.attention.adaptation.AttentionAdaptation
import me.moiclathy.dism.attention.adaptation.AttentionFeedbackAction
import me.moiclathy.dism.attention.explanation.AttentionDecisionExplanation
import me.moiclathy.dism.attention.memory.AttentionMemory

/** Full deterministic projection used to reconstruct the attention runtime state. */
data class AttentionReplayState(
    val attentionKey: String,
    val latestAction: String? = null,
    val latestFeedbackAction: AttentionFeedbackAction? = null,
    val latestExplanation: AttentionDecisionExplanation? = null,
    val adaptation: AttentionAdaptation = AttentionAdaptation(attentionKey),
    val memory: AttentionMemory = AttentionMemory(),
    val firstOccurredAt: Instant? = null,
    val lastOccurredAt: Instant? = null,
    val eventCount: Int = 0,
    val decisionCount: Int = 0,
    val feedbackCount: Int = 0,
)
