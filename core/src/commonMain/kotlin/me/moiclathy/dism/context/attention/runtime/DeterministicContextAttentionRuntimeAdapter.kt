package me.moiclathy.dism.context.attention.runtime

/**
 * 确定性的 Context → Attention Runtime Adapter。
 *
 * 职责：
 *
 * Contextual Recommendation Priority
 *             ↓
 * Attention Runtime Priority
 *
 * 不负责：
 *
 * - Recommendation 创建
 * - Attention 创建
 * - Attention Budget
 * - Attention Lifecycle
 * - Attention Feedback
 * - Attention Memory
 * - Notification
 * - Persistence
 */
class DeterministicContextAttentionRuntimeAdapter :
    ContextAttentionRuntimeAdapter {

    override fun evaluate(
        input: ContextAttentionRuntimeInput,
    ): ContextAttentionRuntimeResult {

        val contextualPriority =
            input.contextualPriority
                .coerceIn(0.0, 1.0)

        val delta =
            contextualPriority -
                input.basePriority

        return ContextAttentionRuntimeResult(
            adjustment =
                ContextAttentionRuntimeAdjustment(
                    basePriority =
                        input.basePriority,

                    contextualPriority =
                        contextualPriority,

                    delta =
                        delta,

                    contextMode =
                        input.contextMode,

                    contextConfidence =
                        input.contextConfidence,
                ),

            evaluatedAt =
                input.evaluatedAt,
        )
    }
}
