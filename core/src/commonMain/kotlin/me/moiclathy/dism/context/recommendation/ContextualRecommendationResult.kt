package me.moiclathy.dism.context.recommendation

import kotlin.time.Instant

/**
 * Contextual Recommendation Runtime 结果。
 */
data class ContextualRecommendationResult(
    val adjustment: ContextualRecommendationAdjustment,

    val evaluatedAt: Instant,
)
