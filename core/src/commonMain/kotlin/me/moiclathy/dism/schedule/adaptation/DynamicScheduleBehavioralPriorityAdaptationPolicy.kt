package me.moiclathy.dism.schedule.adaptation

/**
 * 14.6
 *
 * Schedule Priority 自适应边界。
 *
 * 第一版保持保守。
 *
 * ### 命名说明
 *
 * 与 [DynamicScheduleBehavioralPriorityAdaptation] 同理，
 * 这里加 `Behavioral` 以区别于 12.x 已有的
 * `DynamicSchedulePriorityAdaptationPolicy`
 * （那个是 `minimumFactor / ignoredPenalty / engagedBonus`
 * 的倍率模型，并且已经 `@Serializable`）。
 */
data class DynamicScheduleBehavioralPriorityAdaptationPolicy(
    /**
     * 单次最大 Priority 动态变化。
     */
    val maximumDelta: Double = 0.20,

    /**
     * 最低置信度。
     */
    val minimumConfidence: Double = 0.50,

    /**
     * 最少历史样本。
     */
    val minimumSampleCount: Int = 3,

    /**
     * 完成行为对 Priority 的敏感度。
     *
     * 默认略低，因为"经常完成"并不意味着
     * 这个任务本身应该被排得更靠前。
     */
    val completionSensitivity: Double = 0.25,

    /**
     * 推迟行为对 Priority 的敏感度。
     *
     * 第一版先保守地给予正向影响。
     *
     * 14.7 会进一步结合 Context 判断。
     */
    val postponementSensitivity: Double = 0.75,

    /**
     * 取消行为对 Priority 的敏感度。
     *
     * 默认 0。
     *
     * 取消可能来自用户主动取消，也可能来自外部状态变化，
     * 暂时不能直接解释成"应该降低 Priority"。
     */
    val cancellationSensitivity: Double = 0.0,

    /**
     * 决策稳定性对 Priority 的敏感度。
     */
    val stabilitySensitivity: Double = 0.25,

    /**
     * 最近活动对 Priority 的敏感度。
     */
    val recencySensitivity: Double = 0.50,
) {

    init {
        require(maximumDelta >= 0.0)
        require(minimumConfidence in 0.0..1.0)
        require(minimumSampleCount >= 0)

        require(completionSensitivity >= 0.0)
        require(postponementSensitivity >= 0.0)
        require(cancellationSensitivity >= 0.0)
        require(stabilitySensitivity >= 0.0)
        require(recencySensitivity >= 0.0)
    }
}
