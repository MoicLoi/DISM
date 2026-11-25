package me.moiclathy.dism.event.e2e

import kotlin.time.Instant
import me.moiclathy.dism.event.alarm.DynamicAlarmEngine
import me.moiclathy.dism.event.constraint.TimeConstraint
import me.moiclathy.dism.event.constraint.TimeConstraintEngine
import me.moiclathy.dism.event.duration.EventDuration
import me.moiclathy.dism.event.duration.EventDurationEngine
import me.moiclathy.dism.event.model.Event
import me.moiclathy.dism.event.runtime.EventTimeRuntime
import me.moiclathy.dism.event.runtime.EventTimeRuntimePersistence
import me.moiclathy.dism.event.runtime.EventTimeRuntimePersistenceEvent
import me.moiclathy.dism.event.runtime.EventTimeRuntimeRecovery
import me.moiclathy.dism.event.slack.EventSlackEngine
import me.moiclathy.dism.event.temporal.TemporalStateEngine

/**
 * Time-flow E2E Runner。
 *
 * 只负责把已有模块连接起来。
 */
class EventTimeFlowE2ERunner(
    private val temporalEngine: TemporalStateEngine,
    private val durationEngine: EventDurationEngine,
    private val constraintEngine: TimeConstraintEngine,
    private val slackEngine: EventSlackEngine,
    private val alarmEngine: DynamicAlarmEngine,
    private val runtime: EventTimeRuntime,
    private val persistence: EventTimeRuntimePersistence,
    private val recovery: EventTimeRuntimeRecovery,
) {

    fun run(
        event: Event,
        duration: EventDuration?,
        constraint: TimeConstraint?,
        actualStart: Instant?,
        actualEnd: Instant?,
        evaluatedAt: Instant,
    ): EventTimeFlowE2EResult {

        val steps =
            mutableListOf<EventTimeFlowE2EStep>()

        /*
         * 1. Event
         */
        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.EVENT,

                successful =
                    event.id.value.isNotBlank() &&
                        event.title.isNotBlank(),

                message =
                    "Event accepted: ${event.id.value}",
            )

        /*
         * 2. Temporal
         */
        val temporalEvaluation =
            temporalEngine.evaluate(
                temporal =
                    event.temporal,

                evaluatedAt =
                    evaluatedAt,
            )

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.TEMPORAL,

                successful =
                    true,

                message =
                    "Temporal state: ${temporalEvaluation.state}",
            )

        /*
         * 3. Duration
         */
        val durationResult =
            if (duration != null) {

                durationEngine.resolve(
                    duration
                )

            } else {
                null
            }

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.DURATION,

                successful =
                    true,

                message =
                    if (durationResult != null) {
                        "Duration resolved."
                    } else {
                        "No duration supplied."
                    },
            )

        /*
         * 4. Constraint
         */
        val constraintEvaluation =
            if (constraint != null) {

                constraintEngine.evaluate(
                    constraint =
                        constraint,

                    actualStart =
                        actualStart,

                    actualEnd =
                        actualEnd,

                    evaluatedAt =
                        evaluatedAt,
                )

            } else {
                null
            }

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.CONSTRAINT,

                successful =
                    constraintEvaluation != null,

                message =
                    if (constraintEvaluation != null) {
                        if (constraintEvaluation.satisfied) {
                            "Time constraint satisfied."
                        } else {
                            "Time constraint evaluated with violations."
                        }
                    } else {
                        "No time constraint supplied."
                    },
            )

        /*
         * 5. Slack
         *
         * 如果没有 Constraint，
         * 当前阶段无法计算真正的 constraint slack。
         */
        val slackEvaluation =
            if (
                constraint != null &&
                constraintEvaluation != null &&
                actualStart != null
            ) {

                val boundary =
                    constraint.end
                        ?: constraint.start

                slackEngine.evaluate(
                    boundary =
                        boundary,

                    actual =
                        actualStart,

                    evaluatedAt =
                        evaluatedAt,
                )

            } else {
                null
            }

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.SLACK,

                successful =
                    slackEvaluation != null,

                message =
                    if (slackEvaluation != null) {
                        "Slack status: ${slackEvaluation.status}"
                    } else {
                        "Slack could not be evaluated."
                    },
            )

        /*
         * 6. Dynamic Alarm
         */
        val alarmEvaluation =
            when {

                event.temporal
                    is me.moiclathy.dism.event.model.EventTemporal.At -> {

                    val start =
                        event.temporal.start

                    alarmEngine.beforeStart(
                        startAt =
                            start,

                        evaluatedAt =
                            evaluatedAt,
                    )
                }

                event.temporal
                    is me.moiclathy.dism.event.model.EventTemporal.Interval -> {

                    val start =
                        event.temporal.start

                    alarmEngine.beforeStart(
                        startAt =
                            start,

                        evaluatedAt =
                            evaluatedAt,
                    )
                }

                else -> {
                    null
                }
            }

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.ALARM,

                successful =
                    true,

                message =
                    if (
                        alarmEvaluation?.generated == true
                    ) {
                        "Dynamic alarm generated."
                    } else {
                        "No dynamic alarm generated."
                    },
            )

        /*
         * 7. Runtime
         */
        runtime.start(
            evaluatedAt
        )

        val runtimeEvaluation =
            runtime.tick(
                me.moiclathy.dism.event.runtime.EventTimeRuntimeInput(
                    evaluatedAt =
                        evaluatedAt,

                    eventCount =
                        1,
                )
            )

        val runtimeState =
            runtimeEvaluation.currentState

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.RUNTIME,

                successful =
                    runtimeState ==
                        me.moiclathy.dism.event.runtime.EventTimeRuntimeState.RUNNING,

                message =
                    "Runtime state: $runtimeState",
            )

        /*
         * 8. Persistence
         */
        val persistenceEvent =
            EventTimeRuntimePersistenceEvent(
                id =
                    "${event.id.value}:$evaluatedAt",

                runtimeState =
                    runtimeState,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    runtimeEvaluation.record.processedEventCount,

                generatedAlarmCount =
                    runtimeEvaluation.record.generatedAlarmCount,

                metadata =
                    mapOf(
                        "eventId" to
                            event.id.value,

                        "eventType" to
                            event.type.name,

                        "temporalState" to
                            temporalEvaluation.state.name,
                    ),
            )

        persistence.append(
            persistenceEvent
        )

        val persistedEventCount =
            persistence.all().size

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.PERSISTENCE,

                successful =
                    persistedEventCount > 0,

                message =
                    "Persisted runtime events: $persistedEventCount",
            )

        /*
         * 9. Recovery
         */
        val recoveryResult =
            recovery.recover()

        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.RECOVERY,

                successful =
                    recoveryResult.recovered &&
                        recoveryResult.state == runtimeState,

                message =
                    if (recoveryResult.recovered) {
                        "Runtime recovered as ${recoveryResult.state}."
                    } else {
                        "Runtime recovery failed."
                    },
            )

        /*
         * 10. Final
         *
         * 注意：
         * Contract 必须在 FINAL 加入之后再执行。
         */
        steps +=
            EventTimeFlowE2EStep(
                phase =
                    EventTimeFlowE2EPhase.FINAL,

                successful =
                    true,

                message =
                    "Time-flow E2E completed.",
            )

        val trace =
            EventTimeFlowE2ETrace(
                steps =
                    steps,

                evaluatedAt =
                    evaluatedAt,

                eventId =
                    event.id.value,

                temporalState =
                    temporalEvaluation.state,

                durationResult =
                    durationResult,

                constraintEvaluation =
                    constraintEvaluation,

                slackEvaluation =
                    slackEvaluation,

                alarmEvaluation =
                    alarmEvaluation,

                runtimeState =
                    runtimeState,

                persistedEventCount =
                    persistedEventCount,

                recoveryResult =
                    recoveryResult,
            )

        val errors =
            EventTimeFlowE2EContract.validate(
                trace
            )

        return EventTimeFlowE2EResult(
            trace =
                trace,

            contractSatisfied =
                errors.isEmpty(),

            errors =
                errors,
        )
    }
}
