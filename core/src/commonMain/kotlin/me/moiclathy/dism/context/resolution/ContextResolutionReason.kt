package me.moiclathy.dism.context.resolution

/**
 * 用于解释 Context Resolution 为什么得到当前结果。
 *
 * 后续 Explain / Debug / Event Log 都可以直接使用。
 */
enum class ContextResolutionReason {

    /**
     * Claim 当前仍然有效。
     */
    ACTIVE_CLAIM,

    /**
     * Claim 已经过期，因此被排除。
     */
    EXPIRED_CLAIM,

    /**
     * 原始 confidence 较高。
     */
    HIGH_CONFIDENCE,

    /**
     * Source 类型给予了额外权重。
     */
    SOURCE_PRIORITY,

    /**
     * 同一个 ContextMode 被多个 Source 支持。
     */
    MULTI_SOURCE_SUPPORT,

    /**
     * 最佳候选明显优于其他候选。
     */
    CLEAR_WINNER,

    /**
     * 两个或更多候选分数过于接近。
     */
    AMBIGUOUS_MARGIN,

    /**
     * 没有有效候选。
     */
    NO_ACTIVE_CLAIMS,
}
