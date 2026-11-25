package me.moiclathy.dism.schedule.adaptation

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

/**
 * 14.8
 *
 * 描述一次 Dynamic Adaptation 的可信程度。
 *
 * Confidence != Influence。
 *
 * influence:
 *     系统应该改变多少。
 *
 * confidence:
 *     系统有多确定这种改变是合理的。
 */
data class DynamicScheduleAdaptationConfidence(
    val itemId: DynamicScheduleItemId,

    /**
     * 最终可信度。
     *
     * 0.0 .. 1.0
     */
    val value: Double,

    /**
     * 历史证据可信度。
     */
    val historicalEvidence: Double,

    /**
     * Context 匹配可信度。
     */
    val contextEvidence: Double,

    /**
     * 行为稳定性可信度。
     */
    val stabilityEvidence: Double,

    /**
     * 样本数量贡献。
     */
    val sampleEvidence: Double,

    /**
     * 当前是否允许该 Adaptation 进入下一层。
     */
    val enabled: Boolean,

    /**
     * 人类可读解释。
     */
    val reasons: List<String> = emptyList(),
) {
    init {
        require(value in 0.0..1.0)
        require(historicalEvidence in 0.0..1.0)
        require(contextEvidence in 0.0..1.0)
        require(stabilityEvidence in 0.0..1.0)
        require(sampleEvidence in 0.0..1.0)
    }
}
