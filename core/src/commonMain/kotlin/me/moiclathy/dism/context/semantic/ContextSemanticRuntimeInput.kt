package me.moiclathy.dism.context.semantic

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.semantics.ContextSemanticTarget

/**
 * Context → Semantic Runtime 的输入。
 *
 * Semantic Runtime 已经拥有自己的 Semantic State。
 * Context 只提供当前语境信息。
 */
data class ContextSemanticRuntimeInput(
    val target: ContextSemanticTarget,

    /**
     * 当前 Semantic State 给出的基础语义权重。
     */
    val baseWeight: Double,

    /**
     * 当前 Context 的可信度。
     */
    val contextConfidence: Double,

    /**
     * 计算发生的时间。
     */
    val evaluatedAt: Instant,

    /**
     * 当前 Context。
     *
     * 保留显式字段，避免 Runtime Adapter
     * 必须从 target 中反推。
     */
    val contextMode: ContextMode =
        target.contextMode,
) {

    init {
        require(baseWeight in 0.0..1.0) {
            "baseWeight must be between 0.0 and 1.0."
        }

        require(contextConfidence in 0.0..1.0) {
            "contextConfidence must be between 0.0 and 1.0."
        }
    }
}
