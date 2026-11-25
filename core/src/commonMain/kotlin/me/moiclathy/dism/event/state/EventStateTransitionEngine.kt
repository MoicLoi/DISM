package me.moiclathy.dism.event.state

import kotlin.time.Instant

/**
 * Event State Transition Engine。
 *
 * 负责判断：
 *
 * previousState
 *       ↓
 * currentState
 *
 * 是否形成一个有效 Transition。
 */
interface EventStateTransitionEngine {

    fun transition(
        eventId: String,
        previousState: EventState?,
        currentState: EventState,
        evaluatedAt: Instant,
        reason: String? = null,
    ): EventStateTransitionResult
}
