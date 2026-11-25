package me.moiclathy.dism.schedule.adaptation

import kotlin.time.Duration

/**
 * 14.1
 *
 * 全项目**唯一**的时间衰减公式。
 *
 * 12.x 的 Attention Decay 一直把它藏在
 * [DeterministicDynamicScheduleAttentionDecayEngine] 的私有方法里，
 * 于是任何新功能（例如 14.1 的 `recent_activity` 特征）
 * 都很容易"顺手再写一遍 exp(...)"——
 * 然后系统里就有了两套语义略有差别的衰减。
 *
 * 公式：
 *
 * ```text
 * weight = 2 ^ (-elapsed / halfLife)
 *
 * elapsed = halfLife       → 0.5
 * elapsed = 2 * halfLife   → 0.25
 * ```
 *
 * 并且老历史**永远不会完全归零**，
 * 而是被 [minimumWeight] 兜住。
 */
object DynamicScheduleRecencyDecay {

    fun weight(
        elapsed: Duration,
        halfLife: Duration,

        minimumWeight: Double =
            DynamicScheduleAttentionDecayPolicy()
                .minimumWeight,
    ): Double {

        require(!halfLife.isNegative()) {
            "halfLife must not be negative"
        }

        require(
            minimumWeight in 0.0..1.0,
        ) {
            "minimumWeight must be in [0, 1]"
        }

        if (elapsed <= Duration.ZERO) {
            return 1.0
        }

        val halfLifeSeconds =
            halfLife.inWholeMilliseconds / 1000.0

        if (halfLifeSeconds <= 0.0) {
            return minimumWeight
        }

        val elapsedSeconds =
            elapsed.inWholeMilliseconds / 1000.0

        val rawWeight =
            kotlin.math.exp(
                -kotlin.math.ln(2.0) *
                    elapsedSeconds /
                    halfLifeSeconds,
            )

        return rawWeight.coerceAtLeast(minimumWeight)
    }

    /**
     * 便捷重载：用 [policy] 的 halfLife 与 minimumWeight。
     */
    fun weight(
        elapsed: Duration,
        policy: DynamicScheduleAttentionDecayPolicy,
    ): Double =
        weight(
            elapsed = elapsed,
            halfLife = policy.halfLife,
            minimumWeight = policy.minimumWeight,
        )
}
