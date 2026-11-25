package me.moiclathy.dism.schedule.attention

import me.moiclathy.dism.planning.recommendation.RecommendationKey
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * Resolves a stable semantic identity for a schedule item.
 *
 * The resolver is deliberately injected because a schedule item may
 * originate from different providers and does not necessarily contain
 * enough information to construct a RecommendationKey by itself.
 */
fun interface DynamicScheduleAttentionIdentityResolver {

    fun resolve(
        itemId: DynamicScheduleItemId,
    ): RecommendationKey?
}
