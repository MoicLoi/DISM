package me.moiclathy.dism.context.semantics

import kotlin.time.Instant

/**
 * Context-dependent Semantic Engine。
 *
 * 这是一个纯语义计算边界。
 *
 * 它：
 *
 * Context + Confidence + Target
 *             ↓
 *      Semantic Evaluation
 *
 * 它不：
 *
 * - 修改 Entity
 * - 修改 Relation
 * - 修改 Event
 * - 创建 Recommendation
 * - 发送 Notification
 */
interface ContextSemanticEngine {

    fun evaluate(
        target: ContextSemanticTarget,
        baseWeight: Double,
        contextConfidence: Double,
        now: Instant,
        policy: ContextSemanticPolicy =
            ContextSemanticPolicy(),
    ): ContextSemanticEvaluation
}
