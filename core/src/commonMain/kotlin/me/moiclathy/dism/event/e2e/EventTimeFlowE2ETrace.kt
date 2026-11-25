package me.moiclathy.dism.event.e2e

import kotlin.time.Instant
import me.moiclathy.dism.event.alarm.DynamicAlarm
import me.moiclathy.dism.event.alarm.DynamicAlarmEvaluation
import me.moiclathy.dism.event.duration.EventDurationResult
import me.moiclathy.dism.event.slack.EventSlackEvaluation
import me.moiclathy.dism.event.temporal.TemporalState
import me.moiclathy.dism.event.constraint.TimeConstraintEvaluation
import me.moiclathy.dism.event.runtime.EventTimeRuntimeRecoveryResult
import me.moiclathy.dism.event.runtime.EventTimeRuntimeState

/**
 * Time-flow E2E 执行轨迹。
 */
data class EventTimeFlowE2ETrace(
    val steps: List<EventTimeFlowE2EStep>,

    val evaluatedAt: Instant,

    val eventId: String,

    val temporalState: TemporalState?,

    val durationResult: EventDurationResult?,

    val constraintEvaluation: TimeConstraintEvaluation?,

    val slackEvaluation: EventSlackEvaluation?,

    val alarmEvaluation: DynamicAlarmEvaluation?,

    val runtimeState: EventTimeRuntimeState?,

    val persistedEventCount: Int,

    val recoveryResult: EventTimeRuntimeRecoveryResult?,
) {

    init {
        require(eventId.isNotBlank()) {
            "eventId must not be blank."
        }

        require(persistedEventCount >= 0) {
            "persistedEventCount must not be negative."
        }
    }

    val successful: Boolean
        get() =
            steps.isNotEmpty() &&
                steps.all { it.successful }

    val alarm: DynamicAlarm?
        get() =
            alarmEvaluation?.alarm
}
