package me.moiclathy.dism.context.recommendation

/**
 * Context → Recommendation Adapter。
 *
 * 只负责把 Contextual Semantic Weight
 * 转换成 Recommendation Priority。
 */
interface ContextualRecommendationAdapter {

    fun evaluate(
        input: ContextualRecommendationInput,
    ): ContextualRecommendationResult
}
