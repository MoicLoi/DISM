package me.moiclathy.dism.event.runtime

import kotlin.time.Instant

/**
 * 一次 Time-flow Runtime 执行结果。
 */
data class EventTimeRuntimeEvaluation(
    val previousState: EventTimeRuntimeState,
    val currentState: EventTimeRuntimeState,
    val evaluatedAt: Instant,
    val record: EventTimeRuntimeRecord,
) {

    val stateChanged: Boolean
        get() = previousState != currentState
}
