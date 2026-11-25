package me.moiclathy.dism.context.attention.runtime

/**
 * Context Attention Priority Transformer。
 *
 * 这是纯数学转换边界。
 */
interface ContextAttentionPriorityTransformer {

    fun transform(
        basePriority: Double,
        semanticWeight: Double,
        contextConfidence: Double,
    ): ContextAttentionRuntimeAdjustment
}
