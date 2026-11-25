package me.moiclathy.dism.event.slack

import kotlin.time.Instant

/**
 * Slack Engine。
 */
interface EventSlackEngine {

    /**
     * 根据一个时间边界计算 Slack。
     *
     * boundary:
     *   必须满足的时间边界。
     *
     * actual:
     *   当前/预计时间。
     */
    fun evaluate(
        boundary: Instant?,
        actual: Instant?,
        evaluatedAt: Instant,
    ): EventSlackEvaluation
}
