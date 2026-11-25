package me.moiclathy.dism.context.resolution

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

/**
 * 一次完整的 Context Resolution 结果。
 */
data class ContextResolutionResult(
    val status: ContextResolutionStatus,

    /**
     * RESOLVED 时存在。
     *
     * AMBIGUOUS / UNRESOLVED 时必须为空。
     */
    val resolvedMode: ContextMode? = null,

    /**
     * 最终候选排序。
     *
     * score 从高到低。
     */
    val candidates: List<ContextResolutionCandidate> = emptyList(),

    /**
     * 已过期或低于 minimumConfidence 的 Claim 数量。
     */
    val rejectedClaimCount: Int = 0,

    /**
     * 此次 Resolution 使用的逻辑时间。
     */
    val resolvedAt: Instant,

    val reasons: List<ContextResolutionReason> = emptyList(),
) {

    init {
        when (status) {
            ContextResolutionStatus.RESOLVED -> {
                require(resolvedMode != null) {
                    "resolvedMode is required when status is RESOLVED."
                }
            }

            ContextResolutionStatus.AMBIGUOUS,
            ContextResolutionStatus.UNRESOLVED,
            -> {
                require(resolvedMode == null) {
                    "resolvedMode must be null when context is not resolved."
                }
            }
        }

        require(rejectedClaimCount >= 0) {
            "rejectedClaimCount must not be negative."
        }
    }

    val bestCandidate: ContextResolutionCandidate?
        get() = candidates.firstOrNull()
}
