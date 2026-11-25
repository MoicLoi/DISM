package me.moiclathy.dism.attention.adaptation

import kotlinx.serialization.Serializable

@Serializable
data class AttentionAdaptation(
    val attentionKey: String,
    val suppressionBias: Double = 0.0,
    val repeatBias: Double = 0.0,
    val urgencyBias: Double = 0.0,
    val sampleCount: Int = 0,
) {
    fun clamp(): AttentionAdaptation = copy(
        suppressionBias = suppressionBias.coerceIn(-1.0, 1.0),
        repeatBias = repeatBias.coerceIn(-1.0, 1.0),
        urgencyBias = urgencyBias.coerceIn(-1.0, 1.0),
        sampleCount = sampleCount.coerceAtLeast(0),
    )
}
