package me.moiclathy.dism.event.state

import kotlin.time.Instant

/**
 * 确定性的 Event State Transition Engine。
 *
 * 该 Engine：
 *
 * - 不修改 Event
 * - 不修改数据库
 * - 不创建 Recommendation
 * - 不触发 Attention
 * - 不计算时间
 * - 不执行 Replanning
 *
 * 只负责：
 *
 * State A → State B
 *
 * 是否构成合法 Transition。
 */
class DeterministicEventStateTransitionEngine(
    private val policy:
        EventStateTransitionPolicy =
        EventStateTransitionPolicy(),
) : EventStateTransitionEngine {

    override fun transition(
        eventId: String,
        previousState: EventState?,
        currentState: EventState,
        evaluatedAt: Instant,
        reason: String?,
    ): EventStateTransitionResult {

        /*
         * Event 第一次获得 State。
         */
        if (previousState == null) {

            val transition =
                EventTransition(
                    eventId =
                        eventId,

                    type =
                        EventTransitionType.INITIALIZED,

                    from =
                        null,

                    to =
                        currentState,

                    occurredAt =
                        evaluatedAt,

                    reason =
                        reason,
                )

            return EventStateTransitionResult(
                transition =
                    transition,

                previousState =
                    null,

                currentState =
                    currentState,

                evaluatedAt =
                    evaluatedAt,
            )
        }

        /*
         * State 没有变化。
         */
        if (previousState == currentState) {

            val transition =
                if (policy.emitPersisting) {
                    EventTransition(
                        eventId =
                            eventId,

                        type =
                            EventTransitionType.PERSISTING,

                        from =
                            previousState,

                        to =
                            currentState,

                        occurredAt =
                            evaluatedAt,

                        reason =
                            reason,
                    )
                } else {
                    null
                }

            return EventStateTransitionResult(
                transition =
                    transition,

                previousState =
                    previousState,

                currentState =
                    currentState,

                evaluatedAt =
                    evaluatedAt,
            )
        }

        /*
         * 检查是否允许从终态恢复。
         */
        if (
            !isTransitionAllowed(
                from =
                    previousState,

                to =
                    currentState,
            )
        ) {

            return EventStateTransitionResult(
                transition =
                    null,

                previousState =
                    previousState,

                currentState =
                    previousState,

                evaluatedAt =
                    evaluatedAt,
            )
        }

        val transition =
            EventTransition(
                eventId =
                    eventId,

                type =
                    EventTransitionType.CHANGED,

                from =
                    previousState,

                to =
                    currentState,

                occurredAt =
                    evaluatedAt,

                reason =
                    reason,
            )

        return EventStateTransitionResult(
            transition =
                transition,

            previousState =
                previousState,

            currentState =
                currentState,

            evaluatedAt =
                evaluatedAt,
        )
    }

    private fun isTransitionAllowed(
        from: EventState,
        to: EventState,
    ): Boolean {

        if (
            from == EventState.COMPLETED &&
            !policy.allowCompletedReopen
        ) {
            return false
        }

        if (
            from == EventState.CANCELLED &&
            !policy.allowCancelledReopen
        ) {
            return false
        }

        if (
            from == EventState.EXPIRED &&
            to == EventState.PLANNED &&
            !policy.allowExpiredReopen
        ) {
            return false
        }

        return true
    }
}
