package me.moiclathy.dism.context.attention.runtime

import kotlin.time.Instant

/**
 * Context → Attention Runtime 的计算结果。
 *
 * 注意：
 *
 * 这个结果本身不是 AttentionDecision。
 *
 * 它只是告诉 7.x Attention Runtime：
 *
 * “当前 Recommendation 的 Priority
 * 在 Context 影响下应该是多少。”
 */
data class ContextAttentionRuntimeResult(
    val adjustment:
        ContextAttentionRuntimeAdjustment,

    val evaluatedAt: Instant,
)
