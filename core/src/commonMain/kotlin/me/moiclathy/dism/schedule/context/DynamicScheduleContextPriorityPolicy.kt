package me.moiclathy.dism.schedule.context

/**
 * 14.7
 *
 * Context 对 Priority 的影响边界。
 */
data class DynamicScheduleContextPriorityPolicy(
    /**
     * Context 对 Priority 的最大提升。
     *
     * 例如：
     *
     * ```text
     * maximumIncrease = 0.30
     * ```
     *
     * 表示 Context 最多把 factor 推到 1.30。
     */
    val maximumIncrease: Double = 0.30,

    /**
     * Context 对 Priority 的最大降低。
     *
     * 例如：
     *
     * ```text
     * maximumDecrease = 0.50
     * ```
     *
     * 表示最低可以到 0.50。
     */
    val maximumDecrease: Double = 0.50,

    /**
     * REQUIRED Context 不满足时，
     * Priority 可以下降到的最低比例。
     *
     * 注意：
     * 这不是 BLOCKED。
     *
     * 真正的状态阻塞仍由 ContextBinding 层处理。
     */
    val requiredMissingFactor: Double = 0.50,

    /**
     * EXCLUDED Context 激活时，
     * Priority 可以下降到的最低比例。
     */
    val excludedFactor: Double = 0.25,

    /**
     * PREFERRED Context 的最大影响。
     */
    val preferredInfluence: Double = 0.30,

    /**
     * Context match 对影响强度的控制。
     */
    val matchInfluence: Double = 1.0,
) {

    init {
        require(maximumIncrease >= 0.0)
        require(maximumDecrease in 0.0..1.0)
        require(requiredMissingFactor in 0.0..1.0)
        require(excludedFactor in 0.0..1.0)
        require(preferredInfluence >= 0.0)
        require(matchInfluence >= 0.0)
    }
}
