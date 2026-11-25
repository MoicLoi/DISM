package me.moiclathy.dism.planning.recommendation

data class RecommendationLifecycleChange(
    val key: RecommendationKey,
    val lifecycle: RecommendationLifecycle,
    val previous: Recommendation? = null,
    val current: Recommendation? = null,
)
