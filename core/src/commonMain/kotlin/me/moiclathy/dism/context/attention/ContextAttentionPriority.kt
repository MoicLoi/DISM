package me.moiclathy.dism.context.attention

/**
 * Context 影响后的最终 Attention Priority。
 */
data class ContextAttentionPriority(
    val value: Double,
    val adjustment: Double,
) {

    init {
        require(value in 0.0..1.0) {
            "value must be between 0.0 and 1.0."
        }

        require(adjustment in -1.0..1.0) {
            "adjustment must be between -1.0 and 1.0."
        }
    }
}
