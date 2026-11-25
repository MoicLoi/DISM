package me.moiclathy.dism.context.persistence.sqldelight

import me.moiclathy.dism.context.runtime.ContextRuntimeEvent
import me.moiclathy.dism.context.runtime.ContextRuntimePersistence

/**
 * 将 ContextRuntimeEventLog 适配成
 * ContextRuntimePersistence。
 */
class ContextRuntimeEventLogPersistence(
    private val eventLog:
        SqlDelightContextRuntimeEventLog,
) : ContextRuntimePersistence {

    override fun persist(
        events: Iterable<ContextRuntimeEvent>,
    ) {
        eventLog.appendAll(events)
    }

    override fun loadEvents():
        List<ContextRuntimeEvent> {
        return eventLog.all()
    }
}
