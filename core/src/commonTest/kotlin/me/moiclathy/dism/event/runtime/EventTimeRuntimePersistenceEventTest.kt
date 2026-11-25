package me.moiclathy.dism.event.runtime

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventTimeRuntimePersistenceEventTest {

    private val evaluatedAt =
        Instant.parse(
            "2026-09-16T10:00:00Z"
        )

    @Test
    fun eventShouldStoreValues() {

        val event =
            EventTimeRuntimePersistenceEvent(
                id =
                    "runtime-1",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    5,

                generatedAlarmCount =
                    1,

                metadata =
                    mapOf(
                        "source" to "test",
                    ),
            )

        assertEquals(
            "runtime-1",
            event.id,
        )

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            event.runtimeState,
        )

        assertEquals(
            5,
            event.processedEventCount,
        )

        assertEquals(
            1,
            event.generatedAlarmCount,
        )

        assertEquals(
            "test",
            event.metadata["source"],
        )
    }

    @Test
    fun blankIdShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            EventTimeRuntimePersistenceEvent(
                id =
                    " ",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    0,

                generatedAlarmCount =
                    0,
            )
        }
    }

    @Test
    fun negativeProcessedCountShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            EventTimeRuntimePersistenceEvent(
                id =
                    "runtime-1",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    -1,

                generatedAlarmCount =
                    0,
            )
        }
    }

    @Test
    fun alarmCountCannotExceedProcessedCount() {

        assertFailsWith<IllegalArgumentException> {

            EventTimeRuntimePersistenceEvent(
                id =
                    "runtime-1",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    1,

                generatedAlarmCount =
                    2,
            )
        }
    }

    @Test
    fun equalityShouldBeDeterministic() {

        val first =
            EventTimeRuntimePersistenceEvent(
                id =
                    "runtime-1",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    5,

                generatedAlarmCount =
                    1,
            )

        val second =
            EventTimeRuntimePersistenceEvent(
                id =
                    "runtime-1",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    5,

                generatedAlarmCount =
                    1,
            )

        assertEquals(
            first,
            second,
        )
    }
}
