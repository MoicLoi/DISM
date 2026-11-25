package me.moiclathy.dism.schedule.adaptation

/**
 * 14.4
 *
 * 第一版完全确定性的 Dynamic Weight Adapter。
 *
 * 特点：
 *
 * - 不使用随机数
 * - 不使用 LLM
 * - 不修改 Schedule
 * - 不修改 Attention
 * - 不修改数据库
 * - 相同 Behavioral Model + Policy 永远得到相同结果
 *
 * ### 公式
 *
 * ```text
 * delta = value × sensitivity × confidence × maximumDelta
 * delta = clamp(delta, -maximumDelta, +maximumDelta)
 * ```
 *
 * `maximumDelta` 同时是**缩放系数**和**上限**，
 * 这一点值得单独说明：
 *
 * ```text
 * 0.82 × 1.0 × 0.80 × 0.20 = 0.1312
 * ```
 *
 * 如果 `maximumDelta` 只做 clamp，那么：
 *
 * ```text
 * 0.82 × 1.0 × 0.80 = 0.656
 * ```
 *
 * 一个"倾向 0.82"的行为会直接把权重推走 65%，
 * 而 policy 里写着"最多 0.20"。把上限同时当作缩放系数，
 * 才能让"最多 0.20"这句话在所有输入下都成立。
 *
 * `confidence` 也必须进公式，而不只是当开关：
 *
 * ```text
 * 1 / 1 次完成 → strength 1.0，confidence 0.1
 * → 1.0 × 1.0 × 0.10 × 0.20 = 0.02
 * ```
 *
 * 只有一条样本时几乎不动权重。
 *
 * ### 关于 STABILITY / RECENCY
 *
 * [DynamicScheduleBehavioralAdaptation] 里这两个字段在
 * "没有证据"和"数值很低"之间有歧义，但 14.3 已经保证
 * "有稳定性特征就一定有 HIGH/LOW 其中之一"，
 * 所以这里的 0.0 只可能来自"完全没有历史"，
 * 不额外加 `hasEvidence` 门槛。
 */
class DeterministicDynamicScheduleWeightAdapter :
    DynamicScheduleWeightAdapter {

    override fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        policy:
            DynamicScheduleWeightAdaptationPolicy,
    ): DynamicScheduleWeightAdaptation {

        if (
            behavioral.sampleCount <
            policy.minimumSampleCount
        ) {
            return disabled(
                behavioral = behavioral,
            )
        }

        if (
            behavioral.confidence <
            policy.minimumConfidence
        ) {
            return disabled(
                behavioral = behavioral,
            )
        }

        /**
         * confidence 在这里再次作为缩放因子。
         *
         * 例如：
         *
         * confidence = 0.60
         *
         * 那么即使行为非常明显，
         * 最终调整幅度也只使用 60%。
         */
        val confidenceScale =
            behavioral.confidence
                .coerceIn(0.0, 1.0)

        val deltas =
            mapOf(
                DynamicScheduleWeightDimension.COMPLETION to
                    scale(
                        value =
                            behavioral.completionTendency,
                        sensitivity =
                            policy.completionSensitivity,
                        confidence =
                            confidenceScale,
                        maximumDelta =
                            policy.maximumDelta,
                    ),

                DynamicScheduleWeightDimension.POSTPONEMENT to
                    scale(
                        value =
                            behavioral.postponementTendency,
                        sensitivity =
                            policy.postponementSensitivity,
                        confidence =
                            confidenceScale,
                        maximumDelta =
                            policy.maximumDelta,
                    ),

                DynamicScheduleWeightDimension.CANCELLATION to
                    scale(
                        value =
                            behavioral.cancellationTendency,
                        sensitivity =
                            policy.cancellationSensitivity,
                        confidence =
                            confidenceScale,
                        maximumDelta =
                            policy.maximumDelta,
                    ),

                DynamicScheduleWeightDimension.STABILITY to
                    scale(
                        value =
                            behavioral.decisionStability,
                        sensitivity =
                            policy.stabilitySensitivity,
                        confidence =
                            confidenceScale,
                        maximumDelta =
                            policy.maximumDelta,
                    ),

                DynamicScheduleWeightDimension.RECENCY to
                    scale(
                        value =
                            behavioral.recentActivity,
                        sensitivity =
                            policy.recencySensitivity,
                        confidence =
                            confidenceScale,
                        maximumDelta =
                            policy.maximumDelta,
                    ),
            )

        return DynamicScheduleWeightAdaptation(
            itemId =
                behavioral.itemId,

            deltas =
                deltas,

            confidence =
                behavioral.confidence,

            sampleCount =
                behavioral.sampleCount,

            enabled = true,
        )
    }

    private fun scale(
        value: Double,
        sensitivity: Double,
        confidence: Double,
        maximumDelta: Double,
    ): Double {

        return (
            value *
                sensitivity *
                confidence *
                maximumDelta
            )
            .coerceIn(
                -maximumDelta,
                maximumDelta,
            )
    }

    private fun disabled(
        behavioral:
            DynamicScheduleBehavioralAdaptation,
    ): DynamicScheduleWeightAdaptation {

        return DynamicScheduleWeightAdaptation(
            itemId =
                behavioral.itemId,

            deltas =
                DynamicScheduleWeightDimension
                    .entries
                    .associateWith { 0.0 },

            confidence =
                behavioral.confidence,

            sampleCount =
                behavioral.sampleCount,

            enabled = false,
        )
    }
}
