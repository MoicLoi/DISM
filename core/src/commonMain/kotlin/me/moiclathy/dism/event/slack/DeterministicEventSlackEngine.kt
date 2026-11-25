package me.moiclathy.dism.event.slack

import kotlin.time.Instant
import kotlin.time.Duration

/**
 * 确定性的 Slack Engine。
 *
 * Slack = boundary - actual
 */
class DeterministicEventSlackEngine(
    private val policy: EventSlackPolicy =
        EventSlackPolicy(),
) : EventSlackEngine {

    override fun evaluate(
        boundary: Instant?,
        actual: Instant?,
        evaluatedAt: Instant,
    ): EventSlackEvaluation {

        if (boundary == null || actual == null) {
            return EventSlackEvaluation(
                slack = null,
                status = EventSlackStatus.UNKNOWN,
                evaluatedAt = evaluatedAt,
            )
        }

        val slackDuration =
            boundary - actual

        val slack =
            EventSlack(
                duration = slackDuration,
            )

        val status =
            when {
                slackDuration < Duration.ZERO ->
                    EventSlackStatus.OVERDUE

                slackDuration == Duration.ZERO ->
                    EventSlackStatus.ZERO

                slackDuration <= policy.criticalThreshold ->
                    EventSlackStatus.CRITICAL

                else ->
                    EventSlackStatus.AVAILABLE
            }

        return EventSlackEvaluation(
            slack = slack,
            status = status,
            evaluatedAt = evaluatedAt,
        )
    }
}
