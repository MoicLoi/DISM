package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.5
 *
 * 描述 Behavioral / Weight Adaptation 对 Attention 的影响。
 *
 * 注意：
 *
 * 这个对象不是最终 Attention。
 *
 * 它只是告诉 Attention Runtime：
 *
 * ```text
 * "这个 Item 当前应该获得多少额外的 Attention influence。"
 * ```
 *
 * ### 与 12.x Priority Adaptation 的关系
 *
 * 12.x 的 `DynamicSchedulePriorityAdapter` 是另一条
 * Attention → Priority 的通道，14.5 **不复用、不覆盖**它。
 * 这里产出的是"影响量"，由 14.6+ 决定怎么落到实际决策上。
 *
 * ### 与规格文本的唯一偏差
 *
 * 14.5 规格里 `itemId` 写的是 `String`，并明确注明
 * "等实际接入现有 Session 时，再统一改成 DynamicScheduleItemId"。
 * 本项目已经有 [DynamicScheduleItemId]，这里直接采用它，
 * 以免 14.4 / 14.5 之间出现两种 id 类型。
 */
data class DynamicScheduleAttentionAdaptation(
    val itemId: DynamicScheduleItemId,

    /**
     * 最终 Attention influence 的变化量。
     *
     * 正数：
     *
     * ```text
     * 更容易进入 Attention。
     * ```
     *
     * 负数：
     *
     * ```text
     * 更不容易进入 Attention。
     * ```
     *
     * 0：
     *
     * ```text
     * 不产生适配。
     * ```
     */
    val attentionDelta: Double,

    /**
     * 本次适配的置信度。
     */
    val confidence: Double,

    /**
     * 支撑该适配的历史样本数量。
     */
    val sampleCount: Int,

    /**
     * 是否允许进入 Attention 层。
     */
    val enabled: Boolean,
) {

    init {
        require(attentionDelta.isFinite())
        require(confidence in 0.0..1.0)
        require(sampleCount >= 0)
    }

    /**
     * 是否真的产生了偏移。
     */
    val isNeutral: Boolean
        get() = attentionDelta == 0.0
}
