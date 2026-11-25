package me.moiclathy.dism.schedule.adaptation

/**
 * 14.5
 *
 * Attention Runtime 真正需要消费的最小信息。
 *
 * 这是 14.5 与 Attention Runtime 之间的边界对象。
 *
 * ### 为什么不让 Runtime 直接吃 Adaptation
 *
 * [DynamicScheduleAttentionAdaptation] 带着 `itemId` /
 * `sampleCount` / 历史模型细节，那是**解释用**的信息。
 * Runtime 只应该关心：
 *
 * ```text
 * 偏移多少、有多大把握、算不算数
 * ```
 *
 * 把这条边界画清楚，14.6 接 Runtime 时就不会出现
 * "Runtime 顺手读了 sampleCount 做第二次判断"这种耦合。
 */
data class DynamicScheduleAttentionInfluence(
    val delta: Double,
    val confidence: Double,
    val enabled: Boolean,
) {

    init {
        require(delta.isFinite())
        require(confidence in 0.0..1.0)
    }

    companion object {

        fun from(
            adaptation:
                DynamicScheduleAttentionAdaptation,
        ): DynamicScheduleAttentionInfluence =
            DynamicScheduleAttentionInfluence(
                delta =
                    adaptation.attentionDelta,

                confidence =
                    adaptation.confidence,

                enabled =
                    adaptation.enabled,
            )
    }
}
