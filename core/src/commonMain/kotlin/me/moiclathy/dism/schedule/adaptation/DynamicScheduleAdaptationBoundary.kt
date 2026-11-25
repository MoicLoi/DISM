package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.9
 *
 * 描述某个 Adaptation 在当前边界下最终允许产生的影响。
 *
 * Boundary 不判断“应该不应该调整”。
 *
 * 它只回答：
 *
 *     “即使允许调整，最多允许调整多少？”
 */
data class DynamicScheduleAdaptationBoundary(
    val itemId: DynamicScheduleItemId,

    /**
     * 原始 Adaptation 影响。
     */
    val requestedDelta: Double,

    /**
     * 经过边界限制后的实际允许影响。
     */
    val allowedDelta: Double,

    /**
     * 当前 Confidence。
     */
    val confidence: Double,

    /**
     * 是否触发了单次变化限制。
     */
    val limitedBySingleChange: Boolean,

    /**
     * 是否触发了累计变化限制。
     */
    val limitedByCumulativeChange: Boolean,

    /**
     * 是否触发了频率限制。
     */
    val limitedByFrequency: Boolean,

    /**
     * 是否被用户明确设置保护。
     */
    val limitedByExplicitUserSetting: Boolean,

    /**
     * 是否允许继续进入 Decision Layer。
     */
    val enabled: Boolean,

    val reasons: List<String> = emptyList(),
) {
    init {
        require(requestedDelta.isFinite())
        require(allowedDelta.isFinite())
        require(confidence in 0.0..1.0)
    }
}
