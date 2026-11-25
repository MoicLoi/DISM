package me.moiclathy.dism.attention.adaptation

data class AdaptiveAttentionPolicy(
    val basePriority: Double,
    val adaptation: AttentionAdaptation,
) {
    fun effectivePriority(): Double = (
        basePriority
            - adaptation.suppressionBias
            + adaptation.repeatBias
            + adaptation.urgencyBias
    ).coerceIn(0.0, 1.0)
}
