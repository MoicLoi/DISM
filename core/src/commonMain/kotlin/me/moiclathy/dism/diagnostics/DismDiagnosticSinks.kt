package me.moiclathy.dism.diagnostics

import me.moiclathy.dism.api.DismPublicApi

/**
 * Standard diagnostic sink implementations.
 */
@DismPublicApi
object DismDiagnosticSinks {

    /**
     * Discards diagnostic events.
     *
     * Useful when an application does not want diagnostics.
     */
    val none:
        DismDiagnosticSink =
        DismDiagnosticSink {
            // Intentionally empty.
        }

    /**
     * Keeps the latest events in memory.
     *
     * Intended primarily for development and tests.
     */
    fun inMemory(
        capacity: Int = 256,
    ): InMemoryDismDiagnosticSink =
        InMemoryDismDiagnosticSink(
            capacity = capacity,
        )
}

/**
 * Simple bounded in-memory diagnostic collector.
 */
@DismPublicApi
class InMemoryDismDiagnosticSink(
    private val capacity: Int = 256,
) : DismDiagnosticSink {

    private val events =
        ArrayDeque<DismDiagnosticEvent>()

    init {
        require(capacity > 0) {
            "capacity must be > 0"
        }
    }

    override fun emit(
        event: DismDiagnosticEvent,
    ) {
        if (events.size >= capacity) {
            events.removeFirst()
        }

        events.addLast(event)
    }

    fun snapshot():
        List<DismDiagnosticEvent> =
        events.toList()

    fun clear() {
        events.clear()
    }
}
