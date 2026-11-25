package me.moiclathy.dism.schedule.adaptation

/**
 * 14.4
 *
 * Dynamic Weight Adaptation 的**安全边界**。
 *
 * 第一版故意采用非常保守的默认值：
 *
 * ```text
 * 单个维度最多变化 0.20
 * 置信度低于 0.50 不允许适配
 * 历史样本少于 3 条不允许适配
 * ```
 *
 * 后续 14.8 / 14.9 会进一步完善 confidence / boundary。
 *
 * ### 为什么必须有 maximumDelta
 *
 * 没有它的话：
 *
 * ```text
 * completionTendency = 0.82  →  COMPLETION +0.82
 * ```
 *
 * 历史行为会直接把权重打爆，而且是**无法回退**的——
 * 权重变了会改变决策，决策又写进历史，历史再改变权重。
 *
 * 加上 `maximumDelta = 0.20` 之后：
 *
 * ```text
 * 0.82 × 1.0 × 0.80 × 0.20 = 0.1312
 * ```
 *
 * 单次适配最多只能移动一个维度的 20%。
 */
data class DynamicScheduleWeightAdaptationPolicy(
    /**
     * 单个维度最大权重变化。
     */
    val maximumDelta: Double = 0.20,

    /**
     * 允许开始适配的最低置信度。
     */
    val minimumConfidence: Double = 0.50,

    /**
     * 允许开始适配的最少历史样本。
     */
    val minimumSampleCount: Int = 3,

    /**
     * Completion 行为对 COMPLETION weight 的最大影响。
     */
    val completionSensitivity: Double = 1.0,

    /**
     * Postponement 行为对 POSTPONEMENT weight 的最大影响。
     */
    val postponementSensitivity: Double = 1.0,

    /**
     * Cancellation 行为对 CANCELLATION weight 的最大影响。
     */
    val cancellationSensitivity: Double = 1.0,

    /**
     * Stability 对 STABILITY weight 的最大影响。
     */
    val stabilitySensitivity: Double = 1.0,

    /**
     * Recent Activity 对 RECENCY weight 的最大影响。
     */
    val recencySensitivity: Double = 1.0,
) {

    init {
        require(maximumDelta >= 0.0) {
            "maximumDelta must be >= 0"
        }

        require(minimumConfidence in 0.0..1.0) {
            "minimumConfidence must be in [0, 1]"
        }

        require(minimumSampleCount >= 0) {
            "minimumSampleCount must be >= 0"
        }

        require(completionSensitivity >= 0.0)
        require(postponementSensitivity >= 0.0)
        require(cancellationSensitivity >= 0.0)
        require(stabilitySensitivity >= 0.0)
        require(recencySensitivity >= 0.0)
    }

    /**
     * 一个 Behavioral Model 是否满足进入适配的最低门槛。
     */
    fun admits(
        behavioral:
            DynamicScheduleBehavioralAdaptation,
    ): Boolean =
        behavioral.sampleCount >= minimumSampleCount &&
            behavioral.confidence >= minimumConfidence
}
