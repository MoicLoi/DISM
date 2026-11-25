package me.moiclathy.dism.context.e2e

/**
 * 9.15 Unified Context → Semantic → Recommendation → Attention
 * E2E 所经历的阶段。
 */
enum class ContextSemanticAttentionE2EPhase {

    /**
     * Context 输入。
     */
    CONTEXT,

    /**
     * Context → Semantic。
     */
    SEMANTIC,

    /**
     * Semantic → Recommendation。
     */
    RECOMMENDATION,

    /**
     * Recommendation → Attention。
     */
    ATTENTION,

    /**
     * 最终结果。
     */
    FINAL,
}
