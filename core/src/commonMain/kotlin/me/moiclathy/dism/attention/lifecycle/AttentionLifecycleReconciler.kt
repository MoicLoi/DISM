package me.moiclathy.dism.attention.lifecycle

import me.moiclathy.dism.planning.recommendation.Recommendation
import me.moiclathy.dism.planning.recommendation.RecommendationLifecycle
import me.moiclathy.dism.planning.recommendation.RecommendationLifecycleChange
import kotlin.time.Duration
import kotlin.time.Instant

/**
 * Converts recommendation lifecycle into attention/delivery decisions.
 *
 * Important distinction:
 * - Recommendation lifecycle answers "does this semantic consequence exist?"
 * - Attention lifecycle answers "should we consume the user's attention now?"
 *
 * The reconciler is stateless. Persisted AttentionRecord is supplied by the
 * caller, so the result is deterministic and can later be backed by SQLite.
 */
class AttentionLifecycleReconciler(
    private val defaultRepeatAfter: Duration? = null,
) {

    fun reconcile(
        recommendationChanges: List<RecommendationLifecycleChange>,
        previousRecords: Map<AttentionKey, AttentionRecord> = emptyMap(),
        now: Instant,
    ): List<AttentionDecision> {
        return recommendationChanges.map { change ->
            val key = AttentionKey(change.key)
            val previousRecord = previousRecords[key]

            when (change.lifecycle) {
                RecommendationLifecycle.CREATED -> {
                    val recommendation = requireNotNull(change.current)
                    AttentionDecision(
                        key = key,
                        action = AttentionAction.SHOW,
                        reason = "New recommendation",
                        record = shown(
                            previous = previousRecord,
                            key = key,
                            recommendation = recommendation,
                            now = now,
                        ),
                    )
                }

                RecommendationLifecycle.UPDATED -> {
                    val recommendation = requireNotNull(change.current)
                    val previous = previousRecord

                    // An update is normally worth surfacing once, but it is
                    // still a new semantic revision rather than a duplicate.
                    AttentionDecision(
                        key = key,
                        action = AttentionAction.SHOW,
                        reason = "Recommendation content changed",
                        record = shown(
                            previous = previous,
                            key = key,
                            recommendation = recommendation,
                            now = now,
                        ),
                    )
                }

                RecommendationLifecycle.RESOLVED -> AttentionDecision(
                    key = key,
                    action = AttentionAction.RESOLVED,
                    reason = "Recommendation source risk resolved",
                    record = terminal(
                        previous = previousRecord,
                        key = key,
                        lifecycle = AttentionLifecycle.RESOLVED,
                    ),
                )

                RecommendationLifecycle.EXPIRED -> AttentionDecision(
                    key = key,
                    action = AttentionAction.EXPIRED,
                    reason = "Recommendation source event expired",
                    record = terminal(
                        previous = previousRecord,
                        key = key,
                        lifecycle = AttentionLifecycle.EXPIRED,
                    ),
                )
            }
        }
    }

    /**
     * Evaluate an already-existing recommendation when no lifecycle change was
     * emitted. This is the key deduplication primitive for repeated evaluation.
     */
    fun evaluateExisting(
        recommendation: Recommendation,
        record: AttentionRecord?,
        now: Instant,
        force: Boolean = false,
    ): AttentionDecision {
        val key = AttentionKey(
            recommendationKey = requireNotNull(recommendation.key) {
                "Attention requires a stable RecommendationKey"
            },
        )

        val previous = record
        if (previous == null) {
            return AttentionDecision(
                key = key,
                action = AttentionAction.SHOW,
                reason = "Recommendation has not been shown before",
                record = shown(null, key, recommendation, now),
            )
        }

        if (previous.lifecycle == AttentionLifecycle.RESOLVED ||
            previous.lifecycle == AttentionLifecycle.EXPIRED
        ) {
            return AttentionDecision(
                key = key,
                action = AttentionAction.SUPPRESS,
                reason = "Recommendation is terminal",
                record = previous,
            )
        }

        if (previous.snoozedUntil?.let { now < it } == true && !force) {
            return AttentionDecision(
                key = key,
                action = AttentionAction.SNOOZED,
                reason = "Recommendation is snoozed",
                record = previous,
            )
        }

        val fingerprint = fingerprint(recommendation)
        val unchanged = fingerprint == previous.lastMessageFingerprint
        val cooldownActive = previous.repeatAfter?.let { delay ->
            previous.lastShownAt?.let { last -> now < last + delay }
        } ?: false

        if (!force && unchanged && cooldownActive) {
            return AttentionDecision(
                key = key,
                action = AttentionAction.SUPPRESS,
                reason = "Duplicate recommendation inside attention cooldown",
                record = previous,
            )
        }

        return AttentionDecision(
            key = key,
            action = AttentionAction.REPEAT,
            reason = if (unchanged) {
                "Recommendation is still active and repeat interval elapsed"
            } else {
                "Recommendation content changed without a lifecycle event"
            },
            record = shown(previous, key, recommendation, now),
        )
    }

    private fun shown(
        previous: AttentionRecord?,
        key: AttentionKey,
        recommendation: Recommendation,
        now: Instant,
    ): AttentionRecord {
        return AttentionRecord(
            key = key,
            lifecycle = AttentionLifecycle.SHOWN,
            firstSeenAt = previous?.firstSeenAt ?: now,
            lastShownAt = now,
            shownCount = (previous?.shownCount ?: 0) + 1,
            acknowledgedAt = null,
            snoozedUntil = null,
            lastMessageFingerprint = fingerprint(recommendation),
            repeatAfter = previous?.repeatAfter ?: defaultRepeatAfter,
        )
    }

    private fun terminal(
        previous: AttentionRecord?,
        key: AttentionKey,
        lifecycle: AttentionLifecycle,
    ): AttentionRecord {
        return AttentionRecord(
            key = key,
            lifecycle = lifecycle,
            firstSeenAt = previous?.firstSeenAt,
            lastShownAt = previous?.lastShownAt,
            shownCount = previous?.shownCount ?: 0,
            acknowledgedAt = previous?.acknowledgedAt,
            snoozedUntil = null,
            lastMessageFingerprint = previous?.lastMessageFingerprint,
            repeatAfter = previous?.repeatAfter,
        )
    }

    private fun fingerprint(recommendation: Recommendation): String {
        // Deliberately exclude Recommendation.id: regeneration of an instance
        // must never defeat notification deduplication.
        return buildString {
            append(recommendation.key)
            append('|')
            append(recommendation.type)
            append('|')
            append(recommendation.message)
            append('|')
            append(recommendation.priority)
            append('|')
            append(recommendation.confidence)
            append('|')
            append(recommendation.reasons.joinToString("\u001f"))
        }
    }
}
