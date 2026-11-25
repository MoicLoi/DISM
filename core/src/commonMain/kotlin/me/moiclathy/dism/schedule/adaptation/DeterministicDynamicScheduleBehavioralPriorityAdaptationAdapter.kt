package me.moiclathy.dism.schedule.adaptation

/**
 * 14.6
 *
 * 第一版完全确定性的 Schedule Priority Adapter。
 *
 * 注意：
 *
 * 这里只产生 `priorityDelta`。
 *
 * 不直接修改 `DynamicScheduleItem`。
 *
 * ### 公式
 *
 * ```text
 * rawInfluence = completionTendency    × completionSensitivity
 *              + postponementTendency  × postponementSensitivity
 *              + cancellationTendency  × cancellationSensitivity
 *              + decisionStability     × stabilitySensitivity
 *              + recentActivity        × recencySensitivity
 *
 * priorityDelta = clamp(
 *     rawInfluence × confidence × maximumDelta,
 *     -maximumDelta,
 *     +maximumDelta,
 * )
 * ```
 *
 * 与 14.4 / 14.5 同一个形状，`maximumDelta` 既是缩放系数也是上限。
 *
 * ### 为什么第一版全部是正向
 *
 * 例如：
 *
 * ```text
 * postponementTendency = 0.8
 * ```
 *
 * 得到 `priorityDelta > 0`，含义**不是**：
 *
 * ```text
 * "用户懒，所以应该提高优先级。"
 * ```
 *
 * 而是：
 *
 * ```text
 * "这个 Item 历史上经常进入延期状态，
 *  因此未来决策时暂时增加一点需要考虑它的程度。"
 * ```
 *
 * 两者差别很大，后者只是"多考虑一点"，不是道德判断。
 *
 * `cancellationSensitivity` 默认 0，因为"取消"可能意味着：
 *
 * ```text
 * 用户不需要了        → 该降
 * 外部事件导致取消     → 不该降
 * 已由其他实体完成     → 该降
 * ```
 *
 * 三种解释共存时，14.6 不该过早建立因果关系。
 */
class DeterministicDynamicScheduleBehavioralPriorityAdaptationAdapter :
    DynamicScheduleBehavioralPriorityAdaptationAdapter {

    override fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        weightAdaptation:
            DynamicScheduleWeightAdaptation,

        policy:
            DynamicScheduleBehavioralPriorityAdaptationPolicy,
    ): DynamicScheduleBehavioralPriorityAdaptation {

        if (!weightAdaptation.enabled) {
            return disabled(behavioral)
        }

        if (
            behavioral.sampleCount <
            policy.minimumSampleCount
        ) {
            return disabled(behavioral)
        }

        if (
            behavioral.confidence <
            policy.minimumConfidence
        ) {
            return disabled(behavioral)
        }

        val completion =
            behavioral.completionTendency *
                policy.completionSensitivity

        val postponement =
            behavioral.postponementTendency *
                policy.postponementSensitivity

        val cancellation =
            behavioral.cancellationTendency *
                policy.cancellationSensitivity

        val stability =
            behavioral.decisionStability *
                policy.stabilitySensitivity

        val recency =
            behavioral.recentActivity *
                policy.recencySensitivity

        /*
         * 这里不是简单把所有行为相加后无限增长。
         *
         * 先得到行为影响，再由：
         *
         *     confidence
         *     maximumDelta
         *
         * 统一缩放。
         */
        val rawInfluence =
            completion +
                postponement +
                cancellation +
                stability +
                recency

        val confidenceScale =
            behavioral.confidence
                .coerceIn(0.0, 1.0)

        val priorityDelta =
            (
                rawInfluence *
                    confidenceScale *
                    policy.maximumDelta
                )
                .coerceIn(
                    -policy.maximumDelta,
                    policy.maximumDelta,
                )

        return DynamicScheduleBehavioralPriorityAdaptation(
            itemId =
                behavioral.itemId,

            priorityDelta =
                priorityDelta,

            confidence =
                behavioral.confidence,

            sampleCount =
                behavioral.sampleCount,

            enabled = true,
        )
    }

    private fun disabled(
        behavioral:
            DynamicScheduleBehavioralAdaptation,
    ): DynamicScheduleBehavioralPriorityAdaptation =
        DynamicScheduleBehavioralPriorityAdaptation(
            itemId =
                behavioral.itemId,

            priorityDelta = 0.0,

            confidence =
                behavioral.confidence,

            sampleCount =
                behavioral.sampleCount,

            enabled = false,
        )
}
