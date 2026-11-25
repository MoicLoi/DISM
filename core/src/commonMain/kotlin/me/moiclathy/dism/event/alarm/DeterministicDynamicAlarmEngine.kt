package me.moiclathy.dism.event.alarm

import kotlin.time.Instant
import kotlin.time.Duration

/**
 * 确定性的 Dynamic Alarm Engine。
 *
 * 只负责计算：
 *
 * “什么时候应该产生提醒。”
 *
 * 不负责：
 *
 * - Android Notification
 * - AlarmManager
 * - Attention
 * - Persistence
 * - Replanning
 */
class DeterministicDynamicAlarmEngine(
    private val policy: DynamicAlarmPolicy =
        DynamicAlarmPolicy(),
) : DynamicAlarmEngine {

    override fun beforeStart(
        startAt: Instant?,
        evaluatedAt: Instant,
        leadTime: Duration?,
    ): DynamicAlarmEvaluation {

        if (startAt == null) {
            return DynamicAlarmEvaluation(
                alarm = null,
                evaluatedAt = evaluatedAt,
                generated = false,
                reason = "Start time is missing.",
            )
        }

        val actualLeadTime =
            leadTime ?: policy.defaultLeadTime

        require(!actualLeadTime.isNegative()) {
            "leadTime must not be negative."
        }

        val triggerAt =
            startAt - actualLeadTime

        if (triggerAt <= evaluatedAt) {
            return DynamicAlarmEvaluation(
                alarm = null,
                evaluatedAt = evaluatedAt,
                generated = false,
                reason =
                    "Alarm trigger time has already been reached.",
            )
        }

        return DynamicAlarmEvaluation(
            alarm =
                DynamicAlarm(
                    type =
                        DynamicAlarmType.BEFORE_START,

                    triggerAt =
                        triggerAt,

                    reason =
                        "Event is approaching its start time.",

                    leadTime =
                        actualLeadTime,
                ),

            evaluatedAt =
                evaluatedAt,

            generated =
                true,
        )
    }

    override fun beforeDeadline(
        deadlineAt: Instant?,
        evaluatedAt: Instant,
        leadTime: Duration?,
    ): DynamicAlarmEvaluation {

        if (deadlineAt == null) {
            return DynamicAlarmEvaluation(
                alarm = null,
                evaluatedAt = evaluatedAt,
                generated = false,
                reason = "Deadline is missing.",
            )
        }

        val actualLeadTime =
            leadTime ?: policy.defaultLeadTime

        require(!actualLeadTime.isNegative()) {
            "leadTime must not be negative."
        }

        val triggerAt =
            deadlineAt - actualLeadTime

        if (triggerAt <= evaluatedAt) {
            return DynamicAlarmEvaluation(
                alarm = null,
                evaluatedAt = evaluatedAt,
                generated = false,
                reason =
                    "Deadline alarm trigger time has already been reached.",
            )
        }

        return DynamicAlarmEvaluation(
            alarm =
                DynamicAlarm(
                    type =
                        DynamicAlarmType.BEFORE_DEADLINE,

                    triggerAt =
                        triggerAt,

                    reason =
                        "Event is approaching its deadline.",

                    leadTime =
                        actualLeadTime,
                ),

            evaluatedAt =
                evaluatedAt,

            generated =
                true,
        )
    }

    override fun evaluateSlack(
        boundary: Instant?,
        actual: Instant?,
        evaluatedAt: Instant,
    ): DynamicAlarmEvaluation {

        if (boundary == null || actual == null) {
            return DynamicAlarmEvaluation(
                alarm = null,
                evaluatedAt = evaluatedAt,
                generated = false,
                reason =
                    "Boundary or actual time is missing.",
            )
        }

        val slack =
            boundary - actual

        if (
            slack < Duration.ZERO &&
            policy.allowSlackExhaustedAlarm
        ) {
            return DynamicAlarmEvaluation(
                alarm =
                    DynamicAlarm(
                        type =
                            DynamicAlarmType.SLACK_EXHAUSTED,

                        triggerAt =
                            evaluatedAt,

                        reason =
                            "Time slack has been exhausted.",
                    ),

                evaluatedAt =
                    evaluatedAt,

                generated =
                    true,
            )
        }

        if (
            slack >= Duration.ZERO &&
            slack <= policy.criticalSlackThreshold &&
            policy.allowCriticalSlackAlarm
        ) {
            return DynamicAlarmEvaluation(
                alarm =
                    DynamicAlarm(
                        type =
                            DynamicAlarmType.SLACK_CRITICAL,

                        triggerAt =
                            evaluatedAt,

                        reason =
                            "Time slack has entered the critical range.",
                    ),

                evaluatedAt =
                    evaluatedAt,

                generated =
                    true,
            )
        }

        return DynamicAlarmEvaluation(
            alarm = null,
            evaluatedAt = evaluatedAt,
            generated = false,
            reason =
                "Time slack is outside the alarm range.",
        )
    }
}
