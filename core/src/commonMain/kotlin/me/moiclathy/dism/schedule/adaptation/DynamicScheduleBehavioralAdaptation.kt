package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.3
 *
 * 历史行为形成的结构化 Behavioral Adaptation Model。
 *
 * ### 它描述倾向，不描述改动
 *
 * ```text
 * "这个对象过去表现出了什么稳定行为"   ← 14.3 到这里
 * "以后应该怎么改权重"                 ← 14.4 才决定
 * ```
 *
 * 具体怎么调整 Priority / Attention 属于 14.4+。
 *
 * ### 关于"没有信号时字段为 0.0"
 *
 * 几个数值字段在缺少对应信号时都是 `0.0`。
 *
 * 对阈值型倾向（完成 / 推迟 / 取消）这是**准确的**：
 * `completion_rate = 0.4` 确实意味着"没有完成倾向"。
 *
 * 对稳定性 / 活跃度则要小心——`0.0` 读起来像"非常不稳定"，
 * 但真实含义是"没有证据"。因此消费方**必须**先看
 * [hasEvidence]：没有证据时这些数值没有意义。
 *
 * 14.2 的双向语义保证了"只要有稳定性特征，就一定有
 * HIGH 或 LOW 其中之一"，所以 `0.0` 只可能出现在
 * 完全没有历史的时候。
 */
data class DynamicScheduleBehavioralAdaptation(
    val itemId: DynamicScheduleItemId,

    /**
     * 历史完成倾向。越高表示历史上越容易完成。
     */
    val completionTendency: Double = 0.0,

    /**
     * 历史推迟倾向。
     */
    val postponementTendency: Double = 0.0,

    /**
     * 历史取消倾向。
     */
    val cancellationTendency: Double = 0.0,

    /**
     * 决策稳定程度。越高表示过去的决策状态越稳定。
     */
    val decisionStability: Double = 0.0,

    /**
     * 最近活动程度。
     */
    val recentActivity: Double = 0.0,

    /**
     * 对**整个模型**的置信度。
     *
     * 它不是某一条 feature 的 confidence，
     * 而是按样本量加权的综合结果。
     */
    val confidence: Double = 0.0,

    /**
     * 支撑这个模型的历史样本数量。
     */
    val sampleCount: Int = 0,
) {

    init {
        require(completionTendency in 0.0..1.0)
        require(postponementTendency in 0.0..1.0)
        require(cancellationTendency in 0.0..1.0)
        require(decisionStability in 0.0..1.0)
        require(recentActivity in 0.0..1.0)
        require(confidence in 0.0..1.0)
        require(sampleCount >= 0)
    }

    /**
     * 是否存在足够的历史证据。
     */
    val hasEvidence: Boolean
        get() = sampleCount > 0

    /**
     * 是否已经达到基本的适应置信度。
     *
     * 这里**只描述模型状态**，
     * 不代表允许自动修改 Priority。
     */
    val isReliable: Boolean
        get() = hasEvidence && confidence >= 0.5

    /**
     * 是否表现出任何明显倾向。
     */
    val hasTendencies: Boolean
        get() =
            completionTendency > 0.0 ||
                postponementTendency > 0.0 ||
                cancellationTendency > 0.0
}
