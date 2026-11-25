package me.moiclathy.dism.semantic

/**
 * 对某条状态在当前 Context 下的语义评价。
 */
data class SemanticEvaluation(
    val relevance: Double,
    val urgency: Double,
    val confidence: Double,
    val consequence: Double,
) {
    init {
        require(relevance in 0.0..1.0)
        require(urgency in 0.0..1.0)
        require(confidence in 0.0..1.0)
        require(consequence in 0.0..1.0)
    }

    /**
     * 第一版简单权重。
     *
     * 后续可以替换为更复杂的 Dynamic Weight Engine，
     * 现在先确保整个链路能够工作。
     */
    val weight: Double
        get() = relevance * urgency * confidence * consequence
}