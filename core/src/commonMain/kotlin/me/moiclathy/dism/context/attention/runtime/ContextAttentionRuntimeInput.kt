package me.moiclathy.dism.context.attention.runtime

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * Context → Attention Runtime 输入。
 *
 * 这里已经不是单纯的 Context Semantic Weight。
 *
 * 上游已经完成：
 *
 * Semantic
 *     ↓
 * Context Semantic
 *     ↓
 * Contextual Recommendation
 *
 * 本对象负责把最终的 Recommendation Priority
 * 交给 Attention Runtime。
 */
data class ContextAttentionRuntimeInput(
    /**
     * 稳定的 Attention Key。
     *
     * 应当来自已有 RecommendationKey / AttentionKey。
     */
    val attentionKey: String,

    /**
     * Recommendation 原始 Priority。
     */
    val basePriority: Double,

    /**
     * 经过 Context 调整后的 Priority。
     */
    val contextualPriority: Double,

    /**
     * 当前 Context。
     */
    val contextMode: ContextMode,

    /**
     * 当前 Context 的可信度。
     */
    val contextConfidence: Double,

    /**
     * 评估时间。
     */
    val evaluatedAt: Instant,
) {

    init {
        require(attentionKey.isNotBlank()) {
            "attentionKey must not be blank."
        }

        require(basePriority in 0.0..1.0) {
            "basePriority must be between 0.0 and 1.0."
        }

        require(contextualPriority in 0.0..1.0) {
            "contextualPriority must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }
    }
}
