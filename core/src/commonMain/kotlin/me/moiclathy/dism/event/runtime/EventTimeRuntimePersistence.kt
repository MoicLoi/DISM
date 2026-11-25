package me.moiclathy.dism.event.runtime

/**
 * Time-flow Runtime 持久化接口。
 */
interface EventTimeRuntimePersistence {

    /**
     * 保存一条 Runtime 事件。
     */
    fun append(
        event: EventTimeRuntimePersistenceEvent,
    )

    /**
     * 获取全部 Runtime 事件。
     *
     * 顺序由持久化实现保证为 occurredAt / id 顺序。
     */
    fun all():
        List<EventTimeRuntimePersistenceEvent>

    /**
     * 获取最后一条 Runtime 事件。
     */
    fun latest():
        EventTimeRuntimePersistenceEvent?

    /**
     * 清空 Runtime 事件。
     *
     * 主要用于测试、重置和开发环境。
     */
    fun clear()
}
