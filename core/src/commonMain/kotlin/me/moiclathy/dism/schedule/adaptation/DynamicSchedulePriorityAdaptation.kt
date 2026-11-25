package me.moiclathy.dism.schedule.adaptation

import kotlinx.serialization.Serializable
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 12.x Attention 历史 → Priority 倍率。
 *
 * 14.11 起需要 `@Serializable`：Adaptation Persistence 把整个
 * 适配结果按 JSON 落盘，14.12 再按同样结构重放。
 */
@Serializable
data class DynamicSchedulePriorityAdaptation(
    val itemId: DynamicScheduleItemId,

    /**
     * Schedule 自己给出的原始 priority。
     */
    val basePriority: Double,

    /**
     * Attention 历史产生的调整系数。
     *
     * 0.0..2.0
     *
     * 1.0 = 不调整
     * <1.0 = 降低注意力优先级
     * >1.0 = 提高注意力优先级
     */
    val adaptationFactor: Double,

    /**
     * 最终给 Attention Runtime 使用的 priority。
     */
    val effectivePriority: Double,

    /**
     * 历史行为对结果的解释。
     */
    val reasons: List<String> = emptyList(),
) {
    init {
        require(basePriority in 0.0..1.0)
        require(adaptationFactor in 0.0..2.0)
        require(effectivePriority in 0.0..1.0)
    }
}
