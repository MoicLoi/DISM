package me.moiclathy.dism.context.semantic

import kotlin.time.Instant
import me.moiclathy.dism.context.semantics.ContextSemanticEvaluation

/**
 * Context → Semantic Runtime 的计算结果。
 */
data class ContextSemanticRuntimeResult(
    val adjustment: ContextSemanticRuntimeAdjustment,

    /**
     * 保留 9.6 的原始 Semantic Evaluation，
     * 方便解释和后续诊断。
     */
    val evaluation: ContextSemanticEvaluation,

    val evaluatedAt: Instant,
)
