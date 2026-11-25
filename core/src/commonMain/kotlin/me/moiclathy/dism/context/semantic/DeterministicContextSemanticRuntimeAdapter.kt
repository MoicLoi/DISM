package me.moiclathy.dism.context.semantic

import me.moiclathy.dism.context.semantics.ContextSemanticEngine
import me.moiclathy.dism.context.semantics.ContextSemanticPolicy

/**
 * 9.6 Context Semantic Engine 的 Runtime Adapter。
 *
 * 职责：
 *
 * Context Semantic Engine
 *          ↓
 * Runtime-compatible result
 *
 * 不负责：
 *
 * - Semantic State mutation
 * - Recommendation
 * - Attention
 * - Persistence
 * - Notification
 */
class DeterministicContextSemanticRuntimeAdapter(
    private val engine: ContextSemanticEngine,

    private val policy: ContextSemanticPolicy =
        ContextSemanticPolicy(),
) : ContextSemanticRuntimeAdapter {

    override fun evaluate(
        input: ContextSemanticRuntimeInput,
    ): ContextSemanticRuntimeResult {

        val evaluation =
            engine.evaluate(
                target = input.target,
                baseWeight = input.baseWeight,
                contextConfidence =
                    input.contextConfidence,
                now = input.evaluatedAt,
                policy = policy,
            )

        val adjustedWeight =
            evaluation.score.effectiveWeight
                .coerceIn(0.0, 1.0)

        val delta =
            adjustedWeight -
                input.baseWeight

        val adjustment =
            ContextSemanticRuntimeAdjustment(
                baseWeight =
                    input.baseWeight,

                adjustedWeight =
                    adjustedWeight,

                delta =
                    delta,

                contextMode =
                    input.contextMode,

                contextConfidence =
                    input.contextConfidence,
            )

        return ContextSemanticRuntimeResult(
            adjustment = adjustment,

            evaluation =
                evaluation,

            evaluatedAt =
                input.evaluatedAt,
        )
    }
}
