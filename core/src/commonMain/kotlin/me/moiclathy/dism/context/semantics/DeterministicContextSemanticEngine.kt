package me.moiclathy.dism.context.semantics

import kotlin.time.Instant

/**
 * 确定性的 Context Semantic Engine。
 */
class DeterministicContextSemanticEngine :
    ContextSemanticEngine {

    override fun evaluate(
        target: ContextSemanticTarget,
        baseWeight: Double,
        contextConfidence: Double,
        now: Instant,
        policy: ContextSemanticPolicy,
    ): ContextSemanticEvaluation {

        require(baseWeight in 0.0..1.0) {
            "baseWeight must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }

        /*
         * 找到当前 Context + targetType
         * 对应的全部规则。
         */
        val matchedRules =
            policy.rules
                .filter { rule ->
                    rule.contextMode ==
                        target.contextMode &&
                        rule.targetType.equals(
                            target.targetType,
                            ignoreCase = true,
                        )
                }

        /*
         * 多条规则可以同时存在。
         *
         * 这里采用 delta 相加。
         */
        val rawDelta =
            matchedRules.sumOf { rule ->
                rule.weightDelta
            }

        /*
         * Confidence 决定 Context 语义影响
         * 实际能够发挥多少。
         */
        val effectiveDelta =
            rawDelta *
                contextConfidence *
                policy.confidenceMultiplier

        /*
         * 最终权重限制在策略范围内。
         */
        val effectiveWeight =
            (
                baseWeight +
                    effectiveDelta
                ).coerceIn(
                policy.minimumWeight,
                policy.maximumWeight,
            )

        val relation =
            resolveRelation(
                delta = effectiveDelta,
                matchedRules = matchedRules,
            )

        return ContextSemanticEvaluation(
            target = target,
            contextMode =
                target.contextMode,
            contextConfidence =
                contextConfidence,
            score =
                ContextSemanticScore(
                    baseWeight = baseWeight,
                    contextDelta = effectiveDelta,
                    effectiveWeight =
                        effectiveWeight,
                    relation = relation,
                ),
            matchedRules = matchedRules,
            evaluatedAt = now,
        )
    }

    private fun resolveRelation(
        delta: Double,
        matchedRules: List<ContextSemanticRule>,
    ): ContextSemanticRelation {

        if (matchedRules.isEmpty()) {
            return ContextSemanticRelation.NEUTRAL
        }

        if (delta > 0.25) {
            return ContextSemanticRelation.RELEVANT
        }

        if (delta > 0.0) {
            return ContextSemanticRelation.SUPPORTING
        }

        if (delta < -0.25) {
            return ContextSemanticRelation.SUPPRESSING
        }

        if (delta < 0.0) {
            return ContextSemanticRelation.IRRELEVANT
        }

        return ContextSemanticRelation.NEUTRAL
    }
}
