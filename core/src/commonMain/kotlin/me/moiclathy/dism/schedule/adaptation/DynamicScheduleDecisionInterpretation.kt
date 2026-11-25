package me.moiclathy.dism.schedule.adaptation

/**
 * 14.2
 *
 * 历史 Decision Feature 的语义解释。
 *
 * ### Interpretation 不是 Adaptation
 *
 * Interpretation 只回答：
 *
 * ```text
 * "历史行为表现出了什么特征？"
 * ```
 *
 * 不回答：
 *
 * ```text
 * "以后应该怎么修改 Priority？"
 * ```
 *
 * 后者属于 14.3+。
 */
enum class DynamicScheduleDecisionInterpretationType {

    /**
     * 历史上经常完成。
     */
    COMPLETION_TENDENCY,

    /**
     * 历史上经常被推迟。
     */
    POSTPONEMENT_TENDENCY,

    /**
     * 历史上经常被取消。
     */
    CANCELLATION_TENDENCY,

    /**
     * 历史决策状态比较稳定。
     */
    HIGH_DECISION_STABILITY,

    /**
     * 历史决策变化比较明显。
     */
    LOW_DECISION_STABILITY,

    /**
     * 最近仍然有较强历史活动。
     */
    RECENTLY_ACTIVE,

    /**
     * 最近历史活动较弱。
     */
    NOT_RECENTLY_ACTIVE,
}

/**
 * 一个 Feature 的语义解释结果。
 */
data class DynamicScheduleDecisionInterpretationSignal(
    val type: DynamicScheduleDecisionInterpretationType,

    /**
     * 解释强度。
     *
     * ```text
     * 0.0 = 几乎没有这个倾向
     * 1.0 = 非常明显
     * ```
     */
    val strength: Double,

    /**
     * 对这个解释本身的置信度。
     *
     * 与 [strength] 不同：
     *
     * ```text
     * 1 / 1 次完成
     *     → COMPLETION_TENDENCY strength = 1.0
     *     → confidence 仍然只有 0.1
     * ```
     */
    val confidence: Double,

    /**
     * 支撑这个解释的历史样本数。
     */
    val sampleCount: Int,

    /**
     * 产生这个解释的原始 Feature key。
     */
    val sourceFeatureKey: String,
) {

    init {
        require(strength in 0.0..1.0) {
            "strength must be in [0, 1]"
        }

        require(confidence in 0.0..1.0) {
            "confidence must be in [0, 1]"
        }

        require(sampleCount >= 0) {
            "sampleCount must be >= 0"
        }

        require(sourceFeatureKey.isNotBlank()) {
            "sourceFeatureKey must not be blank"
        }
    }
}

/**
 * 一次历史行为解释的完整结果。
 *
 * ### 刻意没有"综合评分"（14.2 §5）
 *
 * 这里**不**提供：
 *
 * ```kotlin
 * behaviorScore = completionRate * 0.4 + stability * 0.3 + ...
 * ```
 *
 * 因为那实际上已经偷偷进入了
 *
 * ```text
 * Feature Interpretation → Adaptive Weight
 * ```
 *
 * 之后很难回答"为什么系统认为这个任务应该提高权重"。
 *
 * 保留"一个 Feature → 一个语义"的对应关系，
 * 14.3 才能明确决定哪些 Interpretation 走向哪些 Adaptation。
 */
data class DynamicScheduleDecisionInterpretation(
    val signals: List<DynamicScheduleDecisionInterpretationSignal> =
        emptyList(),
) {

    val isEmpty: Boolean
        get() = signals.isEmpty()

    val size: Int
        get() = signals.size

    fun contains(
        type: DynamicScheduleDecisionInterpretationType,
    ): Boolean =
        signals.any { it.type == type }

    fun find(
        type: DynamicScheduleDecisionInterpretationType,
    ): DynamicScheduleDecisionInterpretationSignal? =
        signals.firstOrNull { it.type == type }

    fun strength(
        type: DynamicScheduleDecisionInterpretationType,
    ): Double? =
        find(type)?.strength

    fun confidence(
        type: DynamicScheduleDecisionInterpretationType,
    ): Double? =
        find(type)?.confidence

    companion object {

        val EMPTY =
            DynamicScheduleDecisionInterpretation()
    }
}
