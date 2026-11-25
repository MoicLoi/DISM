package me.moiclathy.dism.attention.lifecycle

import me.moiclathy.dism.planning.recommendation.RecommendationKey
import kotlinx.serialization.Serializable

/**
 * Stable delivery identity of a recommendation.
 *
 * RecommendationKey identifies semantic meaning; AttentionKey identifies the
 * notification/delivery thread for that meaning. It deliberately does not
 * contain Recommendation.id.
 */
@Serializable
data class AttentionKey(
    val recommendationKey: RecommendationKey,
)
