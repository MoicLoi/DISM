package me.moiclathy.dism.context.attention

/**
 * Context → Attention Policy。
 */
data class ContextAttentionPolicy(

    /**
     * Context 语义权重对 Attention 的最大影响比例。
     *
     * 例如：
     *
     * semanticWeight = 0.90
     * confidence = 1.0
     * maximumAdjustment = 0.40
     *
     * 则最多产生：
     *
     * +0.36
     */
    val maximumAdjustment: Double = 0.40,

    /**
     * Context Confidence 对调整量的影响。
     */
    val confidenceMultiplier: Double = 1.0,

    /**
     * Semantic Weight 中性点。
     *
     * 0.50 表示：
     *
     * 0.50 → 不调整
     * >0.50 → 提高 Attention
     * <0.50 → 降低 Attention
     */
    val neutralSemanticWeight: Double = 0.50,

    /**
     * 最低 Attention Priority。
     */
    val minimumPriority: Double = 0.0,

    /**
     * 最高 Attention Priority。
     */
    val maximumPriority: Double = 1.0,
) {

    init {
        require(maximumAdjustment >= 0.0) {
            "maximumAdjustment must not be negative."
        }

        require(confidenceMultiplier >= 0.0) {
            "confidenceMultiplier must not be negative."
        }

        require(
            neutralSemanticWeight in 0.0..1.0
        ) {
            "neutralSemanticWeight must be between 0.0 and 1.0."
        }

        require(
            minimumPriority in 0.0..1.0
        ) {
            "minimumPriority must be between 0.0 and 1.0."
        }

        require(
            maximumPriority in 0.0..1.0
        ) {
            "maximumPriority must be between 0.0 and 1.0."
        }

        require(
            minimumPriority <= maximumPriority
        ) {
            "minimumPriority must not exceed maximumPriority."
        }
    }
}
