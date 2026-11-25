package me.moiclathy.dism.schedule.adaptation

/**
 * 14.4
 *
 * Dynamic Schedule 决策权重维度。
 *
 * 这些维度描述的是：
 *
 * ```text
 * "历史行为对未来决策的影响入口"
 * ```
 *
 * 而不是最终 Priority。
 *
 * 也就是说 `COMPLETION` 不是"完成该加多少分"，
 * 而是"完成这类历史行为应该在决策里占多大分量"。
 */
enum class DynamicScheduleWeightDimension {

    /**
     * 历史完成行为对未来决策的影响。
     */
    COMPLETION,

    /**
     * 历史推迟行为对未来决策的影响。
     */
    POSTPONEMENT,

    /**
     * 历史取消行为对未来决策的影响。
     */
    CANCELLATION,

    /**
     * 历史决策稳定性的影响。
     */
    STABILITY,

    /**
     * 最近活动程度的影响。
     */
    RECENCY,
}
