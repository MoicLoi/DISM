package me.moiclathy.dism.diagnostics

import kotlin.test.Test
import kotlin.test.assertEquals

class DismDiagnosticsContractTest {

    @Test
    fun eventsAreDeliveredToSink() {

        val sink =
            DismDiagnosticSinks.inMemory()

        val diagnostics =
            DismDiagnostics(
                sink = sink,
            )

        diagnostics.info(
            message = "Schedule evaluated.",
            component = "schedule",
            operation = "evaluate",
            code = "DISM.SCHEDULE.EVALUATED",
            attributes =
                mapOf(
                    "items" to "3",
                ),
        )

        val events =
            sink.snapshot()

        assertEquals(
            1,
            events.size,
        )

        val event =
            events.single()

        assertEquals(
            DismDiagnosticLevel.INFO,
            event.level,
        )

        assertEquals(
            "DISM.SCHEDULE.EVALUATED",
            event.code,
        )

        assertEquals(
            "3",
            event.attributes["items"],
        )
    }

    @Test
    fun inMemorySinkIsBounded() {

        val sink =
            DismDiagnosticSinks.inMemory(
                capacity = 2,
            )

        sink.emit(
            DismDiagnosticEvent(
                level = DismDiagnosticLevel.INFO,
                message = "one",
            ),
        )

        sink.emit(
            DismDiagnosticEvent(
                level = DismDiagnosticLevel.INFO,
                message = "two",
            ),
        )

        sink.emit(
            DismDiagnosticEvent(
                level = DismDiagnosticLevel.INFO,
                message = "three",
            ),
        )

        val events =
            sink.snapshot()

        assertEquals(
            2,
            events.size,
        )

        assertEquals(
            "two",
            events[0].message,
        )

        assertEquals(
            "three",
            events[1].message,
        )
    }

    @Test
    fun noneSinkDiscardsEvents() {

        val diagnostics =
            DismDiagnostics(
                sink = DismDiagnosticSinks.none,
            )

        diagnostics.error(
            message = "test",
            code = "DISM.TEST.ERROR",
        )

        // The test passes if the no-op sink accepts the event
        // without introducing side effects.
    }
}
