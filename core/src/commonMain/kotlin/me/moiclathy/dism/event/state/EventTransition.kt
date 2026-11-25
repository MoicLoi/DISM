package me.moiclathy.dism.event.state

import kotlin.time.Instant

/**
 * Event State Transition。
 *
 * 它描述：
 *
 * “Event 在某一时刻从什么状态变成什么状态。”
 *
 * Transition 本身不修改 Event。
 */
data class EventTransition(
    val eventId: String,

    val type: EventTransitionType,

    val from: EventState?,

    val to: EventState,

    val occurredAt: Instant,

    val reason: String? = null,
) {

    init {
        require(eventId.isNotBlank()) {
            "eventId must not be blank."
        }

        require(
            type == EventTransitionType.INITIALIZED &&
                from == null ||
                type != EventTransitionType.INITIALIZED &&
                from != null
        ) {
            "INITIALIZED transition requires null from; other transitions require from."
        }

        if (type == EventTransitionType.CHANGED) {
            require(from != to) {
                "CHANGED transition requires different states."
            }
        }

        if (type == EventTransitionType.PERSISTING) {
            require(from == to) {
                "PERSISTING transition requires identical states."
            }
        }

        if (reason != null) {
            require(reason.isNotBlank()) {
                "reason must not be blank."
            }
        }
    }
}
