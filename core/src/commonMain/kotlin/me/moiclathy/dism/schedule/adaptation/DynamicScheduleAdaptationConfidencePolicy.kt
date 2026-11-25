package me.moiclathy.dism.schedule.adaptation

/**
 * 14.8
 *
 * Adaptation Confidence 的确定性计算策略。
 */
data class DynamicScheduleAdaptationConfidencePolicy(
    /**
     * 历史证据权重。
     */
    val historicalWeight: Double = 0.30,

    /**
     * 当前 Context 证据权重。
     */
    val contextWeight: Double = 0.25,

    /**
     * 稳定性权重。
     */
    val stabilityWeight: Double = 0.20,

    /**
     * 样本量权重。
     */
    val sampleWeight: Double = 0.25,

    /**
     * 达到这个样本量以后，
     * sampleEvidence 视为完全成熟。
     */
    val sampleSaturation: Double = 10.0,

    /**
     * 最低 Confidence。
     *
     * 低于此值时，Adaptation 不应该进入
     * 后续自动决策层。
     */
    val minimumConfidence: Double = 0.50,
) {
    init {
        require(historicalWeight >= 0.0)
        require(contextWeight >= 0.0)
        require(stabilityWeight >= 0.0)
        require(sampleWeight >= 0.0)

        require(
            historicalWeight +
                contextWeight +
                stabilityWeight +
                sampleWeight >
                0.0,
        )

        require(sampleSaturation > 0.0)
        require(minimumConfidence in 0.0..1.0)
    }

    val normalizedHistoricalWeight: Double
        get() = historicalWeight / totalWeight

    val normalizedContextWeight: Double
        get() = contextWeight / totalWeight

    val normalizedStabilityWeight: Double
        get() = stabilityWeight / totalWeight

    val normalizedSampleWeight: Double
        get() = sampleWeight / totalWeight

    private val totalWeight: Double
        get() =
            historicalWeight +
                contextWeight +
                stabilityWeight +
                sampleWeight
}
