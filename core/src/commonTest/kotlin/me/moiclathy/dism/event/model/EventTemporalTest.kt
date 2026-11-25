package me.moiclathy.dism.event.model

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventTemporalTest {

    private val start =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    private val end =
        Instant.parse(
            "2026-09-16T10:00:00Z",
        )

    @Test
    fun unscheduledShouldExist() {

        val temporal =
            EventTemporal.Unscheduled

        assertEquals(
            EventTemporal.Unscheduled,
            temporal,
        )
    }

    @Test
    fun pointInTimeShouldPreserveStart() {

        val temporal =
            EventTemporal.At(
                start = start,
            )

        assertEquals(
            start,
            temporal.start,
        )
    }

    @Test
    fun intervalShouldPreserveBounds() {

        val temporal =
            EventTemporal.Interval(
                start = start,
                end = end,
            )

        assertEquals(
            start,
            temporal.start,
        )

        assertEquals(
            end,
            temporal.end,
        )
    }

    @Test
    fun intervalMayHaveEqualStartAndEnd() {

        val temporal =
            EventTemporal.Interval(
                start = start,
                end = start,
            )

        assertEquals(
            start,
            temporal.start,
        )

        assertEquals(
            start,
            temporal.end,
        )
    }

    @Test
    fun intervalCannotEndBeforeStart() {

        assertFailsWith<IllegalArgumentException> {

            EventTemporal.Interval(
                start = end,
                end = start,
            )
        }
    }

    @Test
    fun deadlineShouldPreserveValue() {

        val temporal =
            EventTemporal.Deadline(
                deadline = end,
            )

        assertEquals(
            end,
            temporal.deadline,
        )
    }
}
