package me.moiclathy.dism.semantic.confidence

import me.moiclathy.dism.persistence.repository.EvidenceRepository
import me.moiclathy.dism.state.model.RelationSupport

/**
 * 把一组 RelationSupport 聚合为 RelationState 的综合置信度。
 *
 * 每份支持的有效贡献：
 * evidence.confidence（来源可信度） × semanticConfidence（语义提取可信度）
 *
 * 聚合使用 independent-support approximation：
 * P(至少一份支持成立) = 1 - Π(1 - pᵢ)
 *
 * 语义：
 * 多份一致证据会增强信心，但越接近 1，提升越困难。
 *
 * 注意：这只是独立支持近似。
 * 群聊里三个人复制同一条通知，并不是真正三份独立证据。
 * 以后会做 Evidence correlation / source grouping，
 * 公式只改这一处。
 */
class RelationConfidenceAggregator(
    private val policy: RelationConfidencePolicy =
        RelationConfidencePolicy(),
) {

    fun aggregate(
        supports: Set<RelationSupport>,
        evidenceRegistry: EvidenceRepository,
    ): Double {

        if (supports.isEmpty()) {
            return 0.0
        }

        val contributions =
            supports.mapNotNull { support ->

                val evidence =
                    evidenceRegistry.find(
                        support.evidenceId
                    ) ?: return@mapNotNull null

                (
                    evidence.confidence *
                        support.semanticConfidence
                    )
                    .coerceIn(
                        0.0,
                        1.0,
                    )
            }

        if (contributions.isEmpty()) {
            return 0.0
        }

        /*
         * Independent-support approximation:
         *
         * P(at least one support is valid)
         * = 1 - Π(1 - pᵢ)
         */
        val combined =
            1.0 -
                contributions.fold(1.0) {
                        remaining, contribution ->
                    remaining *
                        (1.0 - contribution)
                }

        return minOf(
            combined,
            policy.maximumConfidence,
        )
    }
}
