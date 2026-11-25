package me.moiclathy.dism.context.e2e

/**
 * 9.15 Unified E2E Contract。
 *
 * 不负责执行。
 * 只负责验证执行结果是否符合架构约束。
 */
object ContextSemanticAttentionE2EContract {

    fun validate(
        trace: ContextSemanticAttentionE2ETrace,
    ): List<String> {

        val errors =
            mutableListOf<String>()

        val requiredPhases =
            listOf(
                ContextSemanticAttentionE2EPhase.CONTEXT,
                ContextSemanticAttentionE2EPhase.SEMANTIC,
                ContextSemanticAttentionE2EPhase.RECOMMENDATION,
                ContextSemanticAttentionE2EPhase.ATTENTION,
                ContextSemanticAttentionE2EPhase.FINAL,
            )

        val actualPhases =
            trace.steps.map { it.phase }

        for (phase in requiredPhases) {
            if (phase !in actualPhases) {
                errors +=
                    "Missing E2E phase: $phase"
            }
        }

        if (trace.steps.any { !it.successful }) {
            errors +=
                "One or more E2E phases failed."
        }

        /*
         * Semantic Weight 必须位于合法范围。
         */
        if (
            trace.contextualSemanticWeight !in
            0.0..1.0
        ) {
            errors +=
                "Contextual semantic weight is out of range."
        }

        /*
         * Recommendation Priority 必须位于合法范围。
         */
        if (
            trace.contextualRecommendationPriority !in
            0.0..1.0
        ) {
            errors +=
                "Contextual recommendation priority is out of range."
        }

        /*
         * Attention Priority 必须位于合法范围。
         */
        if (
            trace.attentionPriority !in
            0.0..1.0
        ) {
            errors +=
                "Attention priority is out of range."
        }

        /*
         * 关键约束：
         *
         * 9.13 的输出必须成为
         * 9.14 的输入。
         *
         * 因此这里要求：
         *
         * Recommendation Priority
         * ==
         * Attention Priority 的输入。
         *
         * 当前 9.14 Adapter 是透明传递，
         * 所以最终 Attention Priority 应与
         * Contextual Recommendation Priority 相同。
         */
        if (
            kotlin.math.abs(
                trace.contextualRecommendationPriority -
                    trace.attentionPriority
            ) > 0.000001
        ) {
            errors +=
                "Attention priority is not derived from contextual recommendation priority."
        }

        /*
         * Context Confidence 也必须贯穿整个链。
         */
        if (
            trace.contextConfidence !in
            0.0..1.0
        ) {
            errors +=
                "Context confidence is out of range."
        }

        return errors
    }

    fun isSatisfied(
        trace: ContextSemanticAttentionE2ETrace,
    ): Boolean =
        validate(trace).isEmpty()
}
