package me.moiclathy.dism.schedule.adaptation

/**
 * 14.5
 *
 * 第一版确定性 Attention Adapter。
 *
 * 不直接操作 AttentionRuntime。
 *
 * ### 公式
 *
 * ```text
 * rawInfluence = recentActivity × recencySensitivity
 *              + postponementTendency × postponementSensitivity
 *
 * attentionDelta = clamp(
 *     rawInfluence × confidence × maximumDelta,
 *     -maximumDelta,
 *     +maximumDelta,
 * )
 * ```
 *
 * 与 14.4 保持同一个形状：`maximumDelta` 既是缩放系数也是上限。
 *
 * ### 为什么 Attention 只用 Recency 和 Postponement
 *
 * Attention 是"要不要现在打扰用户"，比权重敏感得多。
 * 第一版只有两个信号被认为可以安全地直接影响它：
 *
 * ```text
 * recentActivity    最近确实在动这件事
 * postponement      确实被反复推迟
 * ```
 *
 * `completion` / `cancellation` 的敏感度默认都为 0，
 * 它们在 policy 里是可配置的，但默认不参与——
 * 见 [DynamicScheduleAttentionAdaptationPolicy] 的说明。
 *
 * ### postponement 的方向问题
 *
 * `postponement` 不能直接理解成"这个任务应该少提醒"。
 * 第一版只把它当作**正向**的 Attention signal：
 *
 * ```text
 * 经常推迟
 *     ↓
 * 系统认为未来决策时值得重新关注
 * ```
 *
 * 到底应该是 increase 还是 reduce，留给 14.7 结合 Context 判断。
 */
class DeterministicDynamicScheduleAttentionAdapter :
    DynamicScheduleAttentionAdapter {

    override fun adapt(
        behavioral:
            DynamicScheduleBehavioralAdaptation,

        weightAdaptation:
            DynamicScheduleWeightAdaptation,

        policy:
            DynamicScheduleAttentionAdaptationPolicy,
    ): DynamicScheduleAttentionAdaptation {

        if (!weightAdaptation.enabled) {
            return disabled(
                behavioral = behavioral,
            )
        }

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

        /*
         * Attention 第一版主要使用：
         *
         *     Recent Activity
         *     Postponement
         *
         * 其他行为因素暂时不直接影响 Attention。
         */

        val recencyInfluence =
            behavioral.recentActivity *
                policy.recencySensitivity

        val postponementInfluence =
            behavioral.postponementTendency *
                policy.postponementSensitivity

        val rawInfluence =
            recencyInfluence +
                postponementInfluence

        val confidenceScale =
            behavioral.confidence
                .coerceIn(0.0, 1.0)

        val attentionDelta =
            (
                rawInfluence *
                    confidenceScale *
                    policy.maximumDelta
                )
                .coerceIn(
                    -policy.maximumDelta,
                    policy.maximumDelta,
                )

        return DynamicScheduleAttentionAdaptation(
            itemId =
                behavioral.itemId,

            attentionDelta =
                attentionDelta,

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
    ): DynamicScheduleAttentionAdaptation {

        return DynamicScheduleAttentionAdaptation(
            itemId =
                behavioral.itemId,

            attentionDelta = 0.0,

            confidence =
                behavioral.confidence,

            sampleCount =
                behavioral.sampleCount,

            enabled = false,
        )
    }
}
