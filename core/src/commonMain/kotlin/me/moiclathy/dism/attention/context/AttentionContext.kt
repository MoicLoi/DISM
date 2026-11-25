package me.moiclathy.dism.attention.context

/**
 * Runtime context that changes how much user attention DISM may consume.
 * This is deliberately independent from Android/Desktop notification APIs.
 */
data class AttentionContext(
    val mode: AttentionContextMode = AttentionContextMode.NORMAL,
    val attentionMultiplier: Double = 1.0,
    val maxVisibleItems: Int? = null,
    val suppressNonCritical: Boolean = false,
)

enum class AttentionContextMode {
    NORMAL,
    FOCUSED,
    IN_TRANSIT,
    IN_CLASS,
    RESTING,
    SLEEPING,
    EMERGENCY,
}
