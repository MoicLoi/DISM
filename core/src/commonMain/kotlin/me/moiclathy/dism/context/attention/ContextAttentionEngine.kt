package me.moiclathy.dism.context.attention

import kotlin.time.Instant

/**
 * Context → Attention Engine。
 *
 * 负责：
 *
 * Context
 *   +
 * Confidence
 *   +
 * Semantic Weight
 *   +
 * Base Attention Priority
 *
 *        ↓
 *
 * Context-adjusted Attention Priority
 *
 * 不负责：
 *
 * - Attention Budget
 * - Notification
 * - Lifecycle
 * - Feedback
 * - Persistence
 */
interface ContextAttentionEngine {

    fun evaluate(
        target: ContextAttentionTarget,
        basePriority: Double,
        semanticWeight: Double,
        contextConfidence: Double,
        now: Instant,
        policy: ContextAttentionPolicy =
            ContextAttentionPolicy(),
    ): ContextAttentionEvaluation
}
