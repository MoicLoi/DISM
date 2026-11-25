package me.moiclathy.dism.context.resolution

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextClaim
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.source.ContextSourceId
import me.moiclathy.dism.context.source.ContextSourceResult
import me.moiclathy.dism.context.source.ContextSourceType

/**
 * 确定性的 Context Resolver。
 *
 * 设计原则：
 *
 * 1. 不读取系统时间
 * 2. 不修改 ContextState
 * 3. 不偷偷解决模糊结果
 * 4. 输入相同 → 输出相同
 * 5. 保留原始 Claim / Source 作为证据
 */
class DeterministicContextResolver : ContextResolver {

    override fun resolve(
        sources: List<ContextSourceResult>,
        now: Instant,
        policy: ContextResolutionPolicy,
    ): ContextResolutionResult {

        val activeClaims = mutableListOf<ResolvedSourceClaim>()

        var rejectedCount = 0

        sources.forEach { sourceResult ->

            sourceResult.claims.forEach { claim ->

                if (isExpired(claim, now)) {
                    rejectedCount++
                    return@forEach
                }

                if (claim.confidence < policy.minimumConfidence) {
                    rejectedCount++
                    return@forEach
                }

                activeClaims += ResolvedSourceClaim(
                    sourceId = sourceResult.sourceId,
                    sourceType = sourceResult.sourceType,
                    claim = claim,
                )
            }
        }

        if (activeClaims.isEmpty()) {
            return ContextResolutionResult(
                status = ContextResolutionStatus.UNRESOLVED,
                resolvedMode = null,
                candidates = emptyList(),
                rejectedClaimCount = rejectedCount,
                resolvedAt = now,
                reasons = listOf(
                    ContextResolutionReason.NO_ACTIVE_CLAIMS
                ),
            )
        }

        val candidates = activeClaims
            .groupBy { it.claim.mode }
            .map { (mode, claims) ->
                buildCandidate(
                    mode = mode,
                    values = claims,
                    policy = policy,
                )
            }
            .sortedWith(
                compareByDescending<ContextResolutionCandidate> {
                    it.score
                }.thenByDescending {
                    it.confidence
                }.thenByDescending {
                    it.supportingSourceCount
                }.thenBy {
                    it.mode.name
                }
            )

        val best = candidates.first()

        if (candidates.size == 1) {
            return ContextResolutionResult(
                status = ContextResolutionStatus.RESOLVED,
                resolvedMode = best.mode,
                candidates = candidates,
                rejectedClaimCount = rejectedCount,
                resolvedAt = now,
                reasons = listOf(
                    ContextResolutionReason.CLEAR_WINNER
                ),
            )
        }

        val second = candidates[1]

        val difference =
            best.score - second.score

        if (difference < policy.ambiguityMargin) {
            return ContextResolutionResult(
                status = ContextResolutionStatus.AMBIGUOUS,
                resolvedMode = null,
                candidates = candidates,
                rejectedClaimCount = rejectedCount,
                resolvedAt = now,
                reasons = listOf(
                    ContextResolutionReason.AMBIGUOUS_MARGIN
                ),
            )
        }

        return ContextResolutionResult(
            status = ContextResolutionStatus.RESOLVED,
            resolvedMode = best.mode,
            candidates = candidates,
            rejectedClaimCount = rejectedCount,
            resolvedAt = now,
            reasons = listOf(
                ContextResolutionReason.CLEAR_WINNER
            ),
        )
    }

    private fun buildCandidate(
        mode: ContextMode,
        values: List<ResolvedSourceClaim>,
        policy: ContextResolutionPolicy,
    ): ContextResolutionCandidate {

        val sourceIds =
            values
                .map { it.sourceId }
                .toSet()

        val sourceTypes =
            values
                .map { it.sourceType }
                .toSet()

        val highestConfidence =
            values.maxOf {
                it.claim.confidence
            }

        /*
         * 每条 Claim 独立计算：
         *
         * effectiveConfidence =
         *     raw confidence × source weight
         */
        val weightedScores =
            values.map {
                (
                    it.claim.confidence *
                        policy.sourceWeight(it.sourceType)
                    ).coerceIn(
                    minimumValue = 0.0,
                    maximumValue = 1.0,
                )
            }

        val strongestWeightedScore =
            weightedScores.maxOrNull()
                ?: 0.0

        /*
         * 多来源一致时给予小幅奖励。
         *
         * 一个 Source 不奖励；
         * 两个 Source +1 bonus；
         * 三个 Source +2 bonus。
         */
        val independentExtraSources =
            (sourceIds.size - 1)
                .coerceAtLeast(0)

        val multiSourceBonus =
            independentExtraSources *
                policy.multiSourceBonus

        val finalScore =
            (
                strongestWeightedScore +
                    multiSourceBonus
                ).coerceIn(
                minimumValue = 0.0,
                maximumValue = 1.0,
            )

        val reasons =
            buildList {

                add(
                    ContextResolutionReason.ACTIVE_CLAIM
                )

                if (highestConfidence >= 0.80) {
                    add(
                        ContextResolutionReason.HIGH_CONFIDENCE
                    )
                }

                if (
                    values.any {
                        policy.sourceWeight(
                            it.sourceType
                        ) != 1.0
                    }
                ) {
                    add(
                        ContextResolutionReason.SOURCE_PRIORITY
                    )
                }

                if (sourceIds.size > 1) {
                    add(
                        ContextResolutionReason.MULTI_SOURCE_SUPPORT
                    )
                }
            }

        return ContextResolutionCandidate(
            mode = mode,
            score = finalScore,
            confidence = highestConfidence,
            supportingSourceCount = sourceIds.size,
            sourceIds = sourceIds,
            sourceTypes = sourceTypes,
            claims = values.map {
                it.claim
            },
            reasons = reasons,
        )
    }

    private fun isExpired(
        claim: ContextClaim,
        now: Instant,
    ): Boolean {

        val expiresAt =
            claim.expiresAt
                ?: return false

        return now >= expiresAt
    }

    private data class ResolvedSourceClaim(
        val sourceId: ContextSourceId,
        val sourceType: ContextSourceType,
        val claim: ContextClaim,
    )
}
