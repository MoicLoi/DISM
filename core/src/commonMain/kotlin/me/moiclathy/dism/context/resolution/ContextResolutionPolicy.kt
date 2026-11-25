package me.moiclathy.dism.context.resolution

import me.moiclathy.dism.context.source.ContextSourceType

/**
 * Context Resolution 的策略参数。
 *
 * Resolver 本身保持确定性；
 * 调参全部通过 Policy 完成。
 */
data class ContextResolutionPolicy(

    /**
     * confidence 低于该值的 Claim 不参与 Resolution。
     */
    val minimumConfidence: Double = 0.25,

    /**
     * 第一名和第二名差距小于此值时，
     * 不自动决定，返回 AMBIGUOUS。
     */
    val ambiguityMargin: Double = 0.08,

    /**
     * 多个独立来源同时支持同一个 Context 时，
     * 每增加一个来源所提供的额外支持。
     */
    val multiSourceBonus: Double = 0.05,

    /**
     * 不同 Source 类型的可信度系数。
     *
     * 这是"默认来源可靠性"，
     * 不是绝对真值。
     */
    val sourceWeights: Map<ContextSourceType, Double> = defaultSourceWeights(),
) {

    init {
        require(minimumConfidence in 0.0..1.0) {
            "minimumConfidence must be between 0.0 and 1.0."
        }

        require(ambiguityMargin in 0.0..1.0) {
            "ambiguityMargin must be between 0.0 and 1.0."
        }

        require(multiSourceBonus >= 0.0) {
            "multiSourceBonus must not be negative."
        }

        sourceWeights.forEach { (type, weight) ->
            require(weight >= 0.0) {
                "Source weight for $type must not be negative."
            }
        }
    }

    fun sourceWeight(
        type: ContextSourceType,
    ): Double {
        return sourceWeights[type] ?: 1.0
    }

    companion object {

        fun defaultSourceWeights(): Map<ContextSourceType, Double> {
            return mapOf(
                ContextSourceType.USER_INPUT to 1.00,
                ContextSourceType.OBSERVATION to 0.95,
                ContextSourceType.SCHEDULE to 0.90,
                ContextSourceType.DERIVED to 0.85,
                ContextSourceType.SYSTEM to 0.80,
            )
        }
    }
}
