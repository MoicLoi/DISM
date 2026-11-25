package me.moiclathy.dism.context.semantic

/**
 * Context → Semantic Runtime Adapter。
 *
 * 这是 9.x 和 Semantic Runtime 之间的边界。
 */
interface ContextSemanticRuntimeAdapter {

    fun evaluate(
        input: ContextSemanticRuntimeInput,
    ): ContextSemanticRuntimeResult
}
