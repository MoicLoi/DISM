package me.moiclathy.dism.context.confidence

import kotlin.time.Instant

/**
 * 确定性的 Context Confidence Engine。
 */
class DeterministicContextConfidenceEngine :
    ContextConfidenceEngine {

    override fun decay(
        confidence: ContextConfidence,
        now: Instant,
        policy: ContextConfidencePolicy,
    ): ContextConfidenceResult {

        val lastUpdatedAt =
            confidence.lastUpdatedAt

        if (lastUpdatedAt == null) {
            return ContextConfidenceResult(
                confidence = confidence.copy(
                    lastUpdatedAt = now,
                ),
                change = null,
                evaluatedAt = now,
            )
        }

        val elapsed =
            (
                now.toEpochMilliseconds() -
                    lastUpdatedAt.toEpochMilliseconds()
                ).coerceAtLeast(0L) / 1000.0

        if (elapsed <= 0.0) {
            return ContextConfidenceResult(
                confidence = confidence,
                change = null,
                evaluatedAt = now,
            )
        }

        val newValue =
            policy.decay(
                value = confidence.value,
                elapsedSeconds = elapsed,
            )

        val delta =
            newValue - confidence.value

        val next =
            confidence.copy(
                value = newValue,
                lastUpdatedAt = now,
            )

        return ContextConfidenceResult(
            confidence = next,
            change =
                if (delta == 0.0) {
                    null
                } else {
                    ContextConfidenceChange(
                        mode = confidence.mode.name,
                        previousValue = confidence.value,
                        newValue = newValue,
                        delta = delta,
                        occurredAt = now,
                        reason = "Time decay.",
                    )
                },
            evaluatedAt = now,
        )
    }

    override fun applyEvidence(
        confidence: ContextConfidence,
        evidence: ContextConfidenceEvidence,
        now: Instant,
        policy: ContextConfidencePolicy,
    ): ContextConfidenceResult {

        /*
         * 过期证据不能增强 Confidence。
         */
        if (evidence.isExpired(now)) {
            return ContextConfidenceResult(
                confidence = confidence,
                change = null,
                rejectedEvidence = listOf(evidence),
                evaluatedAt = now,
            )
        }

        /*
         * 先对旧 Confidence 做时间衰减。
         */
        val decayed =
            decay(
                confidence = confidence,
                now = now,
                policy = policy,
            ).confidence

        /*
         * Evidence strength → boost。
         */
        val boost =
            policy.evidenceBoost(
                evidence.strength
            )

        val newValue =
            (
                decayed.value + boost
                ).coerceIn(
                0.0,
                policy.maximumConfidence,
            )

        val delta =
            newValue - decayed.value

        val next =
            decayed.copy(
                value = newValue,
                lastUpdatedAt = now,
            )

        return ContextConfidenceResult(
            confidence = next,
            change =
                if (delta == 0.0) {
                    null
                } else {
                    ContextConfidenceChange(
                        mode = confidence.mode.name,
                        previousValue = decayed.value,
                        newValue = newValue,
                        delta = delta,
                        occurredAt = now,
                        reason =
                            evidence.reason
                                ?: "New context evidence.",
                    )
                },
            acceptedEvidence = listOf(evidence),
            evaluatedAt = now,
        )
    }
}
