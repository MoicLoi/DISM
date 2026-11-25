package me.moiclathy.dism.event.model

/**
 * Event 的基础类型。
 *
 * 这里只描述 Event 是什么类型，
 * 不描述它当前处于什么 State。
 */
enum class EventType {

    /**
     * 一次性事件。
     *
     * 例如：
     *
     * - 去领取实验服
     * - 去医院
     * - 参加考试
     */
    ONE_TIME,

    /**
     * 课程。
     */
    CLASS,

    /**
     * 考试。
     */
    EXAM,

    /**
     * 会议。
     */
    MEETING,

    /**
     * 出行。
     */
    TRAVEL,

    /**
     * 提醒。
     */
    REMINDER,

    /**
     * 预约。
     */
    APPOINTMENT,

    /**
     * 任务型 Event。
     */
    TASK,

    /**
     * 其他 Event。
     */
    OTHER,
}
