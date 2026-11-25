package me.moiclathy.dism.attention.adaptation

/** Stateless coordinator: persistence belongs to the caller/repository. */
class AttentionAdaptationEngine(
    private val policy: AttentionFeedbackPolicy = AttentionFeedbackPolicy(),
) {
    fun learn(
        current: AttentionAdaptation,
        feedback: AttentionFeedback,
    ): AttentionAdaptation = policy.apply(current, feedback)

    fun rank(
        basePriority: Double,
        adaptation: AttentionAdaptation,
    ): Double = AdaptiveAttentionPolicy(basePriority, adaptation).effectivePriority()
}
