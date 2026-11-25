package me.moiclathy.dism.context.resolution

/**
 * Context Resolution 的最终状态。
 */
enum class ContextResolutionStatus {

    /**
     * 成功得到一个当前最可信的 Context。
     */
    RESOLVED,

    /**
     * 有多个非常接近的候选，无法安全选择。
     */
    AMBIGUOUS,

    /**
     * 没有任何有效 ContextClaim。
     */
    UNRESOLVED,
}
