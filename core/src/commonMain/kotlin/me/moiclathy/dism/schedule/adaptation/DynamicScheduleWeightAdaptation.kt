package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.4
 *
 * 一个 Schedule Item 的动态权重适配结果。
 *
 * ### deltas **不是** Priority
 *
 * 它表示：
 *
 * ```text
 * "这个行为因素在未来决策中的影响应该如何变化。"
 * ```
 *
 * 例如：
 *
 * ```text
 * COMPLETION +0.1312
 * ```
 *
 * 的含义是"完成行为这个因素应该获得更多影响力"，
 * **不是**"这条任务的优先级 +0.13"。
 *
 * 真正改变用户看到什么、任务排在哪里，发生在 14.5+。
 */
data class DynamicScheduleWeightAdaptation(
    val itemId: DynamicScheduleItemId,

    /**
     * 各个行为维度的权重变化量。
     *
     * ```text
     * 正数 = 增强该因素影响
     * 负数 = 降低该因素影响
     * ```
     *
     * 模型本身允许负值（[DynamicScheduleWeightAdaptationPolicy]
     * 的 clamp 是双向的）；第一版的确定性实现只会产出非负值，
     * 因为"行为强度"本身就是非负的。
     */
    val deltas:
        Map<DynamicScheduleWeightDimension, Double>,

    /**
     * 产生该结果时的整体置信度。
     */
    val confidence: Double,

    /**
     * 支撑该结果的历史样本数量。
     */
    val sampleCount: Int,

    /**
     * 是否允许进入后续自适应层。
     *
     * 注意：
     *
     * ```text
     * enabled == true
     * ```
     *
     * **仍然不意味着**应该直接修改 Priority。
     */
    val enabled: Boolean,
) {

    init {
        require(confidence in 0.0..1.0)
        require(sampleCount >= 0)

        deltas.values.forEach {
            require(it.isFinite()) {
                "weight deltas must be finite"
            }
        }
    }

    fun delta(
        dimension: DynamicScheduleWeightDimension,
    ): Double =
        deltas[dimension] ?: 0.0

    /**
     * 所有维度是否都是 0。
     */
    val isNeutral: Boolean
        get() =
            DynamicScheduleWeightDimension
                .entries
                .all { delta(it) == 0.0 }

    /**
     * 绝对值最大的那个维度的偏移量。
     */
    val magnitude: Double
        get() =
            DynamicScheduleWeightDimension
                .entries
                .maxOfOrNull {
                    kotlin.math.abs(delta(it))
                }
                ?: 0.0
}
