package me.moiclathy.dism.semantic.confidence

/**
 * RelationConfidenceAggregator 的配置。
 */
data class RelationConfidencePolicy(

    /**
     * 防止多份相关证据非常快地把置信度堆到 1.0。
     */
    val maximumConfidence: Double = 0.99,
) {
    init {
        require(maximumConfidence in 0.0..1.0)
    }
}
