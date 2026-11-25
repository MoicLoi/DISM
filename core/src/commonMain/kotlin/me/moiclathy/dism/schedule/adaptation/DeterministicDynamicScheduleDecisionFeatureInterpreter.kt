package me.moiclathy.dism.schedule.adaptation

/**
 * 14.2
 *
 * 第一版采用**完全确定性的规则解释**。
 *
 * 暂时不引入：
 *
 * ```text
 * ML
 * LLM
 * 在线学习
 * 自动调权
 * ```
 *
 * 相同 Feature 输入必须得到完全相同的 Interpretation。
 *
 * ### 阈值语义
 *
 * 采用"**超过阈值才产生正向语义**"，而不是把每一个
 * `0.01` 的波动都解释成一种行为：
 *
 * ```text
 * completion_rate   >= 0.70  → COMPLETION_TENDENCY
 * postponement_rate >= 0.30  → POSTPONEMENT_TENDENCY
 * cancellation_rate >= 0.30  → CANCELLATION_TENDENCY
 * ```
 *
 * 而 stability 与 recent_activity 是**双向**的：
 * 阈值之上给正向语义，之下给反向语义。
 * 因为"稳定 / 不稳定"本身就是一对互补判断，
 * 不给出其中一个会让 14.3 无法处理另一半情况。
 */
class DeterministicDynamicScheduleDecisionFeatureInterpreter(
    private val completionThreshold: Double = 0.70,

    private val postponementThreshold: Double = 0.30,

    private val cancellationThreshold: Double = 0.30,

    private val stabilityThreshold: Double = 0.70,

    private val recentActivityThreshold: Double = 0.50,
) : DynamicScheduleDecisionFeatureInterpreter {

    init {
        require(completionThreshold in 0.0..1.0)
        require(postponementThreshold in 0.0..1.0)
        require(cancellationThreshold in 0.0..1.0)
        require(stabilityThreshold in 0.0..1.0)
        require(recentActivityThreshold in 0.0..1.0)
    }

    override fun interpret(
        features: DynamicScheduleDecisionFeatureSet,
    ): DynamicScheduleDecisionInterpretation {

        if (features.isEmpty) {
            return DynamicScheduleDecisionInterpretation.EMPTY
        }

        val signals =
            buildList {

                addThresholdSignal(
                    feature = features["completion_rate"],
                    threshold = completionThreshold,
                    above =
                        DynamicScheduleDecisionInterpretationType
                            .COMPLETION_TENDENCY,
                )

                addThresholdSignal(
                    feature = features["postponement_rate"],
                    threshold = postponementThreshold,
                    above =
                        DynamicScheduleDecisionInterpretationType
                            .POSTPONEMENT_TENDENCY,
                )

                addThresholdSignal(
                    feature = features["cancellation_rate"],
                    threshold = cancellationThreshold,
                    above =
                        DynamicScheduleDecisionInterpretationType
                            .CANCELLATION_TENDENCY,
                )

                addBipolarSignal(
                    feature = features["decision_stability"],
                    threshold = stabilityThreshold,
                    above =
                        DynamicScheduleDecisionInterpretationType
                            .HIGH_DECISION_STABILITY,
                    below =
                        DynamicScheduleDecisionInterpretationType
                            .LOW_DECISION_STABILITY,
                )

                addBipolarSignal(
                    feature = features["recent_activity"],
                    threshold = recentActivityThreshold,
                    above =
                        DynamicScheduleDecisionInterpretationType
                            .RECENTLY_ACTIVE,
                    below =
                        DynamicScheduleDecisionInterpretationType
                            .NOT_RECENTLY_ACTIVE,
                )
            }

        return DynamicScheduleDecisionInterpretation(
            signals = signals,
        )
    }

    /**
     * 只有超过阈值才产生语义，低于阈值什么都不说。
     *
     * 例如 `completion_rate = 0.4` 既不是"经常完成"
     * 也不是"经常不完成"——它只是"没有明显倾向"。
     */
    private fun MutableList<DynamicScheduleDecisionInterpretationSignal>
        .addThresholdSignal(
            feature: DynamicScheduleDecisionFeature?,
            threshold: Double,
            above:
                DynamicScheduleDecisionInterpretationType,
        ) {

        feature ?: return

        if (feature.value < threshold) {
            return
        }

        add(
            DynamicScheduleDecisionInterpretationSignal(
                type = above,

                strength = feature.value,

                confidence = feature.confidence,

                sampleCount = feature.sampleCount,

                sourceFeatureKey = feature.key,
            ),
        )
    }

    /**
     * 双向语义：阈值之上给 [above]，之下给 [below]
     * （强度取补值）。
     */
    private fun MutableList<DynamicScheduleDecisionInterpretationSignal>
        .addBipolarSignal(
            feature: DynamicScheduleDecisionFeature?,
            threshold: Double,
            above:
                DynamicScheduleDecisionInterpretationType,
            below:
                DynamicScheduleDecisionInterpretationType,
        ) {

        feature ?: return

        val isAbove = feature.value >= threshold

        add(
            DynamicScheduleDecisionInterpretationSignal(
                type = if (isAbove) above else below,

                /*
                 * 反向语义的"强度"用补值：
                 * stability = 0.25 → LOW_DECISION_STABILITY = 0.75。
                 */
                strength =
                    if (isAbove) {
                        feature.value
                    } else {
                        1.0 - feature.value
                    },

                confidence = feature.confidence,

                sampleCount = feature.sampleCount,

                sourceFeatureKey = feature.key,
            ),
        )
    }
}
