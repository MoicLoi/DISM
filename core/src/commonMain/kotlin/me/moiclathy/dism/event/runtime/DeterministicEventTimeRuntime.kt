package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

/**
 * 确定性的 Time-flow Runtime。
 *
 * 负责维护 Runtime 生命周期以及 Tick 记录。
 *
 * 它不直接重新实现：
 *
 * - Temporal Engine
 * - Duration Engine
 * - Constraint Engine
 * - Slack Engine
 * - Alarm Engine
 *
 * 这些职责仍属于各自模块。
 */
class DeterministicEventTimeRuntime(
    private val policy: EventTimeRuntimePolicy =
        EventTimeRuntimePolicy(),
) : EventTimeRuntime {

    private var runtimeState =
        EventTimeRuntimeState.STOPPED

    private var lastEvaluatedAt: Instant? =
        null

    override fun start(
        evaluatedAt: Instant,
    ): EventTimeRuntimeEvaluation {

        val previous =
            runtimeState

        runtimeState =
            EventTimeRuntimeState.RUNNING

        lastEvaluatedAt =
            evaluatedAt

        return evaluation(
            previousState =
                previous,

            evaluatedAt =
                evaluatedAt,

            processedEventCount =
                0,

            generatedAlarmCount =
                0,
        )
    }

    override fun pause(
        evaluatedAt: Instant,
    ): EventTimeRuntimeEvaluation {

        val previous =
            runtimeState

        runtimeState =
            EventTimeRuntimeState.PAUSED

        lastEvaluatedAt =
            evaluatedAt

        return evaluation(
            previousState =
                previous,

            evaluatedAt =
                evaluatedAt,

            processedEventCount =
                0,

            generatedAlarmCount =
                0,
        )
    }

    override fun stop(
        evaluatedAt: Instant,
    ): EventTimeRuntimeEvaluation {

        val previous =
            runtimeState

        runtimeState =
            EventTimeRuntimeState.STOPPED

        lastEvaluatedAt =
            evaluatedAt

        return evaluation(
            previousState =
                previous,

            evaluatedAt =
                evaluatedAt,

            processedEventCount =
                0,

            generatedAlarmCount =
                0,
        )
    }

    override fun tick(
        input: EventTimeRuntimeInput,
    ): EventTimeRuntimeEvaluation {

        val previous =
            runtimeState

        if (
            runtimeState !=
            EventTimeRuntimeState.RUNNING
        ) {

            return evaluation(
                previousState =
                    previous,

                evaluatedAt =
                    input.evaluatedAt,

                processedEventCount =
                    0,

                generatedAlarmCount =
                    0,
            )
        }

        if (
            !policy.allowRepeatedEvaluation &&
            lastEvaluatedAt == input.evaluatedAt
        ) {

            return evaluation(
                previousState =
                    previous,

                evaluatedAt =
                    input.evaluatedAt,

                processedEventCount =
                    0,

                generatedAlarmCount =
                    0,
            )
        }

        lastEvaluatedAt =
            input.evaluatedAt

        return evaluation(
            previousState =
                previous,

            evaluatedAt =
                input.evaluatedAt,

            processedEventCount =
                input.eventCount,

            generatedAlarmCount =
                0,
        )
    }

    override fun state():
        EventTimeRuntimeState =
        runtimeState

    private fun evaluation(
        previousState: EventTimeRuntimeState,
        evaluatedAt: Instant,
        processedEventCount: Int,
        generatedAlarmCount: Int,
    ): EventTimeRuntimeEvaluation {

        return EventTimeRuntimeEvaluation(
            previousState =
                previousState,

            currentState =
                runtimeState,

            evaluatedAt =
                evaluatedAt,

            record =
                EventTimeRuntimeRecord(
                    evaluatedAt =
                        evaluatedAt,

                    processedEventCount =
                        processedEventCount,

                    generatedAlarmCount =
                        generatedAlarmCount,
                ),
        )
    }
}
