package me.moiclathy.dism.event.model

/**
 * Event 来源。
 *
 * 用于回答：
 *
 * “这个 Event 是从哪里来的？”
 */
enum class EventSource {

    /**
     * 用户直接输入。
     */
    USER_INPUT,

    /**
     * 从外部课表同步。
     */
    EXTERNAL_SCHEDULE,

    /**
     * 系统根据已有状态推导。
     */
    DERIVED,

    /**
     * 系统内部创建。
     */
    SYSTEM,

    /**
     * 从其他 Event 派生。
     */
    EVENT_DERIVED,

    /**
     * 未知来源。
     */
    UNKNOWN,
}
