package me.moiclathy.dism.event.runtime

/**
 * 内存版 Runtime Persistence。
 *
 * 用于测试和纯 Core 环境。
 */
class InMemoryEventTimeRuntimePersistence :
    EventTimeRuntimePersistence {

    private val events =
        mutableListOf<EventTimeRuntimePersistenceEvent>()

    override fun append(
        event: EventTimeRuntimePersistenceEvent,
    ) {
        events += event
    }

    override fun all():
        List<EventTimeRuntimePersistenceEvent> =
        events.toList()

    override fun latest():
        EventTimeRuntimePersistenceEvent? =
        events.lastOrNull()

    override fun clear() {
        events.clear()
    }
}
