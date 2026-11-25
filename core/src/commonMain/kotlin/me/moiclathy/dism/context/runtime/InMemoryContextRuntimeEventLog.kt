package me.moiclathy.dism.context.runtime

import me.moiclathy.dism.context.model.ContextMode

/**
 * 测试和早期 Runtime 使用的内存 Event Log。
 */
class InMemoryContextRuntimeEventLog :
    ContextRuntimeEventLog {

    private val events =
        mutableListOf<ContextRuntimeEvent>()

    override fun append(
        event: ContextRuntimeEvent,
    ) {
        events += event
    }

    override fun appendAll(
        events: Iterable<ContextRuntimeEvent>,
    ) {
        this.events += events
    }

    override fun eventsFor(
        mode: ContextMode,
    ): List<ContextRuntimeEvent> {
        return events
            .filter {
                it.mode == mode
            }
            .sortedWith(
                compareBy<ContextRuntimeEvent> {
                    it.occurredAt
                }.thenBy {
                    it.id
                }
            )
    }

    override fun all(): List<ContextRuntimeEvent> {
        return events
            .toList()
            .sortedWith(
                compareBy<ContextRuntimeEvent> {
                    it.occurredAt
                }.thenBy {
                    it.id
                }
            )
    }
}
