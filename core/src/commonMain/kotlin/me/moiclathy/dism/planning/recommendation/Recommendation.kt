package me.moiclathy.dism.planning.recommendation

import kotlin.uuid.Uuid

data class Recommendation(
    val id: Uuid = Uuid.random(),
    val type: RecommendationType,
    val message: RecommendationMessage,

    /**
     * 0.0..1.0
     */
    val priority: Double,

    /**
     * 系统对这条建议本身的置信度。
     */
    val confidence: Double,

    /**
     * 为什么产生这条建议。
     *
     * 后续可以用于"为什么提醒我？"
     */
    val reasons: List<String> = emptyList(),

    /**
     * 跨一次重新计算仍然稳定的语义身份。
     * id 是实例身份，不能用于 Recommendation Lifecycle 匹配。
     */
    val key: RecommendationKey? = null,
) {
    init {
        require(priority in 0.0..1.0)
        require(confidence in 0.0..1.0)
    }
}
