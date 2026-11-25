package me.moiclathy.dism.context.confidence

import kotlin.time.Instant

/**
 * Confidence 的一次变化。
 *
 * 这是事件描述，不直接修改外部状态。
 */
data class ContextConfidenceChange(
    val mode: String,
    val previousValue: Double,
    val newValue: Double,
    val delta: Double,
    val occurredAt: Instant,
    val reason: String,
) {

    init {
        require(
            previousValue in 0.0..1.0
        ) {
            "previousValue must be between 0.0 and 1.0."
        }

        require(
            newValue in 0.0..1.0
        ) {
            "newValue must be between 0.0 and 1.0."
        }
    }
}
