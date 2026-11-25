package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.6
 *
 * 描述**历史行为**对 Schedule Priority 的影响。
 *
 * 注意：
 *
 * ```text
 * priorityDelta 不是最终 Priority
 * ```
 *
 * 它只是：
 *
 * ```text
 * "如果未来 Decision Engine 允许使用这份适配，
 *  Priority 可以获得多大的动态影响。"
 * ```
 *
 * ### 为什么叫 `Behavioral…` 而不是规格里的 `DynamicSchedulePriorityAdaptation`
 *
 * 本项目在同一个包 `me.moiclathy.dism.schedule.adaptation` 里
 * **已经有** 12.x 的
 *
 * ```text
 * DynamicSchedulePriorityAdaptation
 *     basePriority / adaptationFactor / effectivePriority
 * ```
 *
 * 那是 Attention 历史 → Priority 的**另一条通道**，
 * 而且 14.7 起的 Context-Sensitive Priority、14.8 Confidence、
 * 14.11 Persistence 都直接消费那个类。
 *
 * 14.6 规格给出的 delta 模型与它同名但语义完全不同，
 * 二者不能同时存在。按"14.x 不得复用或覆盖 12.x 通道"的原则，
 * 这里把 14.x 的这套模型统一加上 `Behavioral` 限定词：
 *
 * ```text
 * 12.x  DynamicSchedulePriorityAdaptation          （保持不变）
 * 14.6  DynamicScheduleBehavioralPriorityAdaptation
 * ```
 *
 * 这样 14.7–14.17 可以完全按规格原文合并，无需任何改名。
 */
data class DynamicScheduleBehavioralPriorityAdaptation(
    val itemId: DynamicScheduleItemId,

    /**
     * Priority 的动态影响量。
     *
     * 正数：
     *
     * ```text
     * 倾向于提高 Priority。
     * ```
     *
     * 负数：
     *
     * ```text
     * 倾向于降低 Priority。
     * ```
     */
    val priorityDelta: Double,

    /**
     * 本次适配的置信度。
     */
    val confidence: Double,

    /**
     * 支撑该结果的历史样本数量。
     */
    val sampleCount: Int,

    /**
     * 是否允许进入后续 Priority Decision Layer。
     */
    val enabled: Boolean,
) {

    init {
        require(priorityDelta.isFinite())
        require(confidence in 0.0..1.0)
        require(sampleCount >= 0)
    }

    /**
     * 是否真的产生了偏移。
     */
    val isNeutral: Boolean
        get() = priorityDelta == 0.0
}
