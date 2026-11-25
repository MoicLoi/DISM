package me.moiclathy.dism.event.state

import kotlin.time.Instant

/**
 * Event State Transition 计算结果。
 */
data class EventStateTransitionResult(
    val transition: EventTransition?,

    val previousState: EventState?,

    val currentState: EventState,

    val evaluatedAt: Instant,
) {

    val changed: Boolean
        get() =
            transition?.type ==
                EventTransitionType.CHANGED ||
                transition?.type ==
                EventTransitionType.INITIALIZED
}
