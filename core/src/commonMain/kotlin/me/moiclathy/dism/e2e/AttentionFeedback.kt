package me.moiclathy.dism.e2e

enum class AttentionFeedbackType { OPENED, COMPLETED, DISMISSED, SNOOZED, IGNORED }

data class AttentionFeedback(
    val attentionKey: String,
    val recommendationKey: String,
    val type: AttentionFeedbackType,
)
