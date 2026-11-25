package me.moiclathy.dism.context.attention.runtime

/**
 * Context → Attention Runtime 边界。
 */
interface ContextAttentionRuntimeAdapter {

    fun evaluate(
        input: ContextAttentionRuntimeInput,
    ): ContextAttentionRuntimeResult
}
