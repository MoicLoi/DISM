package me.moiclathy.dism.context.e2e

import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.runtime.ContextRuntimeEvent
import me.moiclathy.dism.context.runtime.ContextRuntimeEventLog
import me.moiclathy.dism.context.runtime.ContextRuntimeEventType
import me.moiclathy.dism.context.runtime.ContextRuntimeRecovery
import me.moiclathy.dism.context.runtime.ContextRuntimeState

/**
 * Context Runtime E2E Runner。
 *
 * 用于验证：
 *
 * Source
 * -> Resolution
 * -> Transition
 * -> Confidence
 * -> Event
 * -> Persistence
 * -> Recovery
 */
class ContextRuntimeE2ERunner(
    private val eventLog:
        ContextRuntimeEventLog,

    private val recovery:
        ContextRuntimeRecovery =
        ContextRuntimeRecovery(),
) {

    fun run(
        initialMode: ContextMode,
        initialConfidence: Double,
        nextMode: ContextMode,
        nextConfidence: Double,
        firstObservedAt: Instant,
        transitionedAt: Instant,
    ): ContextRuntimeE2EResult {

        val phases =
            mutableListOf<ContextRuntimeE2EPhase>()

        val events =
            mutableListOf<ContextRuntimeEvent>()

        /*
         * ① Source
         *
         * 这里使用 runner 的输入作为已经产生的
         * Context Source 结果。
         *
         * 不在 E2E Runner 内重新实现 Source。
         */
        phases +=
            ContextRuntimeE2EPhase.SOURCE

        /*
         * ② Resolution
         */
        val resolvedEvent =
            ContextRuntimeEvent(
                id = "context-e2e-resolved",
                type =
                    ContextRuntimeEventType.RESOLVED,
                occurredAt =
                    firstObservedAt,
                mode = initialMode,
                confidence =
                    initialConfidence,
            )

        phases +=
            ContextRuntimeE2EPhase.RESOLUTION

        /*
         * ③ Transition
         */
        val transitionEvent =
            ContextRuntimeEvent(
                id = "context-e2e-transition",
                type =
                    ContextRuntimeEventType.TRANSITION,
                occurredAt =
                    transitionedAt,
                previousMode =
                    initialMode,
                mode =
                    nextMode,
                confidence =
                    nextConfidence,
            )

        phases +=
            ContextRuntimeE2EPhase.TRANSITION

        /*
         * ④ Confidence
         */
        val confidenceEvent =
            ContextRuntimeEvent(
                id = "context-e2e-confidence",
                type =
                    ContextRuntimeEventType
                        .CONFIDENCE_CHANGED,
                occurredAt =
                    transitionedAt,
                mode =
                    nextMode,
                confidence =
                    nextConfidence,
            )

        phases +=
            ContextRuntimeE2EPhase.CONFIDENCE

        /*
         * ⑤ Runtime Event
         */
        events +=
            resolvedEvent

        events +=
            transitionEvent

        events +=
            confidenceEvent

        phases +=
            ContextRuntimeE2EPhase.RUNTIME_EVENT

        /*
         * ⑥ Persistence
         */
        eventLog.appendAll(events)

        phases +=
            ContextRuntimeE2EPhase.PERSISTED

        /*
         * ⑦ Recovery
         *
         * 注意这里不是直接使用 Runtime State。
         *
         * 模拟 App 重启：
         *
         * Event Log
         *     ↓
         * Recovery
         *     ↓
         * new State
         */
        val persistedEvents =
            eventLog.all()

        val recoveredState =
            recovery.recover(
                persistedEvents,
            )

        phases +=
            ContextRuntimeE2EPhase.RECOVERY

        /*
         * ⑧ Final State
         */
        phases +=
            ContextRuntimeE2EPhase.FINAL_STATE

        val trace =
            ContextRuntimeE2ETrace(
                phases = phases,
                events = persistedEvents,
                recoveredState =
                    recoveredState,
            )

        val failures =
            ContextRuntimeE2EContract.validate(
                trace,
            )

        return ContextRuntimeE2EResult(
            success =
                failures.isEmpty(),
            trace = trace,
            failures = failures,
        )
    }

    fun recoverOnly():
        ContextRuntimeState {

        return recovery.recover(
            eventLog.all(),
        )
    }
}
