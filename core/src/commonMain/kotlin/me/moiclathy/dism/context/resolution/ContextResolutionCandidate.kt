package me.moiclathy.dism.context.resolution

import me.moiclathy.dism.context.model.ContextClaim
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.source.ContextSourceId
import me.moiclathy.dism.context.source.ContextSourceType

/**
 * 一个经过聚合和评分的 Context 候选。
 *
 * 多个 Source 对相同 ContextMode 的 Claim
 * 会被合并成同一个 Candidate。
 */
data class ContextResolutionCandidate(
    val mode: ContextMode,

    /**
     * 最终用于排序的综合分数。
     *
     * 建议保持在 0.0..1.0。
     */
    val score: Double,

    /**
     * 该 ContextMode 下原始 Claim 中最高的 confidence。
     */
    val confidence: Double,

    /**
     * 有多少独立 Source 支持该 ContextMode。
     */
    val supportingSourceCount: Int,

    /**
     * 支持这一 ContextMode 的 Source ID。
     */
    val sourceIds: Set<ContextSourceId> = emptySet(),

    /**
     * Source 类型。
     */
    val sourceTypes: Set<ContextSourceType> = emptySet(),

    /**
     * 原始 Claims。
     *
     * 保留它们是为了 Explain / Evidence Chain，
     * 而不是重新计算状态。
     */
    val claims: List<ContextClaim> = emptyList(),

    val reasons: List<ContextResolutionReason> = emptyList(),
) {

    init {
        require(score in 0.0..1.0) {
            "Context resolution score must be between 0.0 and 1.0."
        }

        require(confidence in 0.0..1.0) {
            "Context confidence must be between 0.0 and 1.0."
        }

        require(supportingSourceCount >= 0) {
            "supportingSourceCount must not be negative."
        }
    }
}
