package me.moiclathy.dism.schedule.adaptation

/**
 * 14.6
 *
 * Priority Adaptation 与未来 Decision Engine 之间的边界。
 *
 * 与 [DynamicScheduleAttentionInfluence] 对称：
 * Decision Engine 只应该看到"偏移多少、有多大把握、算不算数"，
 * 而不是整个历史模型。
 */
data class DynamicScheduleBehavioralPriorityInfluence(
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
                DynamicScheduleBehavioralPriorityAdaptation,
        ): DynamicScheduleBehavioralPriorityInfluence =
            DynamicScheduleBehavioralPriorityInfluence(
                delta =
                    adaptation.priorityDelta,

                confidence =
                    adaptation.confidence,

                enabled =
                    adaptation.enabled,
            )
    }
}
