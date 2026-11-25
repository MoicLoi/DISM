package me.moiclathy.dism.context.runtime

import me.moiclathy.dism.context.model.ContextMode

/**
 * Context Runtime Event Log。
 */
interface ContextRuntimeEventLog {

    fun append(
        event: ContextRuntimeEvent,
    )

    fun appendAll(
        events: Iterable<ContextRuntimeEvent>,
    )

    fun eventsFor(
        mode: ContextMode,
    ): List<ContextRuntimeEvent>

    fun all(): List<ContextRuntimeEvent>
}
