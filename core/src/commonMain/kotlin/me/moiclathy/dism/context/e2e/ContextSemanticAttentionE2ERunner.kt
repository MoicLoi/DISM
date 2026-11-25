package me.moiclathy.dism.context.e2e

import kotlin.time.Instant
import me.moiclathy.dism.context.attention.runtime.ContextAttentionRuntimeInput
import me.moiclathy.dism.context.attention.runtime.ContextAttentionRuntimeAdapter
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.recommendation.ContextualRecommendationInput
import me.moiclathy.dism.context.recommendation.ContextualRecommendationAdapter
import me.moiclathy.dism.context.recommendation.ContextualRecommendationTarget
import me.moiclathy.dism.context.semantic.ContextSemanticRuntimeInput
import me.moiclathy.dism.context.semantic.ContextSemanticRuntimeAdapter
import me.moiclathy.dism.context.semantics.ContextSemanticTarget

/**
 * 9.15 Unified Context → Semantic → Recommendation → Attention
 * E2E Runner。
 *
 * 注意：
 *
 * Runner 不直接依赖：
 *
 * - SemanticStateEngine
 * - RecommendationEngine
 * - AttentionRuntime
 *
 * 这些组件由上游 Adapter 提供结果。
 *
 * 这样可以避免 E2E 测试层绑定具体实现。
 */
class ContextSemanticAttentionE2ERunner(
    private val semanticAdapter:
        ContextSemanticRuntimeAdapter,

    private val recommendationAdapter:
        ContextualRecommendationAdapter,

    private val attentionAdapter:
        ContextAttentionRuntimeAdapter,
) {

    fun run(
        attentionKey: String,
        targetId: String,
        targetType: String,
        contextMode: ContextMode,
        basePriority: Double,
        baseSemanticWeight: Double,
        contextConfidence: Double,
        evaluatedAt: Instant,
    ): ContextSemanticAttentionE2EResult {

        val steps =
            mutableListOf<ContextSemanticAttentionE2EStep>()

        /*
         * ============================================================
         * Phase 1
         * Context
         * ============================================================
         */

        val contextTarget =
            ContextSemanticTarget(
                targetId =
                    targetId,

                targetType =
                    targetType,

                contextMode =
                    contextMode,
            )

        steps +=
            ContextSemanticAttentionE2EStep(
                phase =
                    ContextSemanticAttentionE2EPhase.CONTEXT,

                successful =
                    contextConfidence in 0.0..1.0,

                message =
                    "Context accepted.",
            )

        if (
            contextConfidence !in
            0.0..1.0
        ) {
            return failure(
                steps = steps,
                attentionKey = attentionKey,
                basePriority = basePriority,
                semanticWeight = baseSemanticWeight,
                recommendationPriority = basePriority,
                attentionPriority = basePriority,
                contextConfidence = contextConfidence,
            )
        }

        /*
         * ============================================================
         * Phase 2
         * Context → Semantic
         * ============================================================
         */

        val semanticResult =
            semanticAdapter.evaluate(
                ContextSemanticRuntimeInput(
                    target =
                        contextTarget,

                    baseWeight =
                        baseSemanticWeight,

                    contextConfidence =
                        contextConfidence,

                    evaluatedAt =
                        evaluatedAt,

                    contextMode =
                        contextMode,
                ),
            )

        val contextualSemanticWeight =
            semanticResult.adjustment.adjustedWeight

        steps +=
            ContextSemanticAttentionE2EStep(
                phase =
                    ContextSemanticAttentionE2EPhase.SEMANTIC,

                successful =
                    contextualSemanticWeight in
                        0.0..1.0,

                message =
                    "Context semantic evaluation completed.",
            )

        /*
         * ============================================================
         * Phase 3
         * Semantic → Recommendation
         * ============================================================
         */

        val recommendationTarget =
            ContextualRecommendationTarget(
                targetId =
                    targetId,

                targetType =
                    targetType,

                contextMode =
                    contextMode,
            )

        val recommendationResult =
            recommendationAdapter.evaluate(
                ContextualRecommendationInput(
                    target =
                        recommendationTarget,

                    basePriority =
                        basePriority,

                    contextualSemanticWeight =
                        contextualSemanticWeight,

                    contextConfidence =
                        contextConfidence,

                    evaluatedAt =
                        evaluatedAt,
                ),
            )

        val contextualRecommendationPriority =
            recommendationResult
                .adjustment
                .adjustedPriority

        steps +=
            ContextSemanticAttentionE2EStep(
                phase =
                    ContextSemanticAttentionE2EPhase.RECOMMENDATION,

                successful =
                    contextualRecommendationPriority in
                        0.0..1.0,

                message =
                    "Contextual recommendation evaluation completed.",
            )

        /*
         * ============================================================
         * Phase 4
         * Recommendation → Attention
         * ============================================================
         */

        val attentionResult =
            attentionAdapter.evaluate(
                ContextAttentionRuntimeInput(
                    attentionKey =
                        attentionKey,

                    basePriority =
                        basePriority,

                    contextualPriority =
                        contextualRecommendationPriority,

                    contextMode =
                        contextMode,

                    contextConfidence =
                        contextConfidence,

                    evaluatedAt =
                        evaluatedAt,
                ),
            )

        val attentionPriority =
            attentionResult
                .adjustment
                .contextualPriority

        steps +=
            ContextSemanticAttentionE2EStep(
                phase =
                    ContextSemanticAttentionE2EPhase.ATTENTION,

                successful =
                    attentionPriority in
                        0.0..1.0,

                message =
                    "Contextual recommendation was passed to Attention Runtime boundary.",
            )

        /*
         * ============================================================
         * Phase 5
         * Final
         * ============================================================
         */

        /*
         * Contract 需要看到完整的 5 个阶段。
         *
         * 因此先放入一个占位的 FINAL step，
         * 用完整 Trace 校验 Contract，
         * 再用真正的校验结果替换它。
         */
        val pendingFinalStep =
            ContextSemanticAttentionE2EStep(
                phase =
                    ContextSemanticAttentionE2EPhase.FINAL,

                successful =
                    true,

                message =
                    "Unified E2E contract evaluation.",
            )

        val trace =
            ContextSemanticAttentionE2ETrace(
                steps =
                    steps + pendingFinalStep,

                attentionKey =
                    attentionKey,

                basePriority =
                    basePriority,

                contextualSemanticWeight =
                    contextualSemanticWeight,

                contextualRecommendationPriority =
                    contextualRecommendationPriority,

                attentionPriority =
                    attentionPriority,

                contextConfidence =
                    contextConfidence,
            )

        val contractErrors =
            ContextSemanticAttentionE2EContract.validate(
                trace,
            )

        val finalStep =
            pendingFinalStep.copy(
                successful =
                    contractErrors.isEmpty(),

                message =
                    if (contractErrors.isEmpty()) {
                        "Unified E2E contract satisfied."
                    } else {
                        contractErrors.joinToString("; ")
                    },
            )

        val finalTrace =
            trace.copy(
                steps =
                    steps + finalStep,
            )

        return ContextSemanticAttentionE2EResult(
            trace =
                finalTrace,

            contractSatisfied =
                contractErrors.isEmpty(),

            errors =
                contractErrors,
        )
    }

    private fun failure(
        steps:
            List<ContextSemanticAttentionE2EStep>,

        attentionKey: String,
        basePriority: Double,
        semanticWeight: Double,
        recommendationPriority: Double,
        attentionPriority: Double,
        contextConfidence: Double,
    ): ContextSemanticAttentionE2EResult {

        val trace =
            ContextSemanticAttentionE2ETrace(
                steps =
                    steps,

                attentionKey =
                    attentionKey,

                basePriority =
                    basePriority,

                contextualSemanticWeight =
                    semanticWeight.coerceIn(
                        0.0,
                        1.0,
                    ),

                contextualRecommendationPriority =
                    recommendationPriority.coerceIn(
                        0.0,
                        1.0,
                    ),

                attentionPriority =
                    attentionPriority.coerceIn(
                        0.0,
                        1.0,
                    ),

                contextConfidence =
                    contextConfidence.coerceIn(
                        0.0,
                        1.0,
                    ),
            )

        val errors =
            listOf(
                "Invalid Context confidence.",
            )

        return ContextSemanticAttentionE2EResult(
            trace =
                trace,

            contractSatisfied =
                false,

            errors =
                errors,
        )
    }
}
