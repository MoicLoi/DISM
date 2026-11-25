package me.moiclathy.dism.planning.recommendation

import kotlin.uuid.Uuid
import kotlinx.serialization.Serializable

/**
 * Stable identity of a recommendation across recomputation.
 *
 * Recommendation.id is an instance id and may change when the recommendation
 * is regenerated. This key identifies the same semantic recommendation.
 */
@Serializable
data class RecommendationKey(
    val type: RecommendationType,
    val eventId: Uuid,
    val requiredEntityId: Uuid,
)
