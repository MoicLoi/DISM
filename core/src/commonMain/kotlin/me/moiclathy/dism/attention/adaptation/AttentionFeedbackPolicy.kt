package me.moiclathy.dism.attention.adaptation

/**
 * Converts user behavior into small, bounded policy adjustments.
 * It deliberately learns slowly so one accidental tap cannot rewrite policy.
 */
class AttentionFeedbackPolicy(
    private val learningRate: Double = 0.15,
) {
    fun apply(
        current: AttentionAdaptation,
        feedback: AttentionFeedback,
    ): AttentionAdaptation {
        val weight = feedback.consequenceWeight.coerceIn(0.0, 1.0)
        val delta = learningRate * weight
        val next = when (feedback.action) {
            AttentionFeedbackAction.OPENED -> current.copy(
                suppressionBias = current.suppressionBias - delta,
                repeatBias = current.repeatBias + delta * 0.35,
            )
            AttentionFeedbackAction.COMPLETED -> current.copy(
                suppressionBias = current.suppressionBias - delta * 0.5,
                urgencyBias = current.urgencyBias - delta * 0.25,
            )
            AttentionFeedbackAction.SNOOZED -> current.copy(
                suppressionBias = current.suppressionBias + delta * 0.35,
                repeatBias = current.repeatBias + delta * 0.2,
            )
            AttentionFeedbackAction.DISMISSED -> current.copy(
                suppressionBias = current.suppressionBias + delta,
                repeatBias = current.repeatBias - delta * 0.5,
            )
            AttentionFeedbackAction.IGNORED -> current.copy(
                suppressionBias = current.suppressionBias + delta * 0.7,
                repeatBias = current.repeatBias - delta * 0.25,
            )
        }
        return next.copy(sampleCount = current.sampleCount + 1).clamp()
    }
}
