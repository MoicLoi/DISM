package me.moiclathy.dism.event.state

/**
 * Event 当前生命周期状态。
 *
 * EventType 描述“它是什么”；
 * EventState 描述“它现在怎么样”。
 */
enum class EventState {

    /**
     * 已计划，但尚未开始。
     */
    PLANNED,

    /**
     * 已经开始，正在进行。
     */
    ACTIVE,

    /**
     * 已完成。
     */
    COMPLETED,

    /**
     * 已取消。
     */
    CANCELLED,

    /**
     * 已过期。
     *
     * 例如截止时间已经过去，
     * 但 Event 没有完成。
     */
    EXPIRED,

    /**
     * 被推迟。
     */
    POSTPONED,
}
