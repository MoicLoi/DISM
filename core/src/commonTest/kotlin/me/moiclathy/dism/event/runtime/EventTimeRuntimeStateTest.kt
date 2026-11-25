package me.moiclathy.dism.event.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventTimeRuntimeStateTest {

    @Test
    fun allStatesShouldExist() {

        assertEquals(
            3,
            EventTimeRuntimeState.entries.size,
        )

        assertEquals(
            EventTimeRuntimeState.STOPPED,
            EventTimeRuntimeState.valueOf("STOPPED"),
        )

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            EventTimeRuntimeState.valueOf("RUNNING"),
        )

        assertEquals(
            EventTimeRuntimeState.PAUSED,
            EventTimeRuntimeState.valueOf("PAUSED"),
        )
    }

    @Test
    fun recordShouldRejectNegativeEventCount() {

        assertFailsWith<IllegalArgumentException> {

            EventTimeRuntimeRecord(
                evaluatedAt =
                    kotlin.time.Instant.parse(
                        "2026-09-16T10:00:00Z"
                    ),

                processedEventCount =
                    -1,

                generatedAlarmCount =
                    0,
            )
        }
    }

    @Test
    fun recordShouldRejectTooManyAlarms() {

        assertFailsWith<IllegalArgumentException> {

            EventTimeRuntimeRecord(
                evaluatedAt =
                    kotlin.time.Instant.parse(
                        "2026-09-16T10:00:00Z"
                    ),

                processedEventCount =
                    1,

                generatedAlarmCount =
                    2,
            )
        }
    }

    @Test
    fun policyShouldRejectNonPositiveTickInterval() {

        assertFailsWith<IllegalArgumentException> {

            EventTimeRuntimePolicy(
                tickInterval =
                    kotlin.time.Duration.ZERO,
            )
        }
    }

    @Test
    fun inputShouldRejectNegativeEventCount() {

        assertFailsWith<IllegalArgumentException> {

            EventTimeRuntimeInput(
                evaluatedAt =
                    kotlin.time.Instant.parse(
                        "2026-09-16T10:00:00Z"
                    ),

                eventCount =
                    -1,
            )
        }
    }
}
