package me.moiclathy.dism.event.temporal

import kotlin.test.Test
import kotlin.test.assertEquals

class TemporalStateTest {

    @Test
    fun allTemporalStatesShouldExist() {

        assertEquals(
            TemporalState.UNSCHEDULED,
            TemporalState.valueOf("UNSCHEDULED"),
        )

        assertEquals(
            TemporalState.UPCOMING,
            TemporalState.valueOf("UPCOMING"),
        )

        assertEquals(
            TemporalState.IN_PROGRESS,
            TemporalState.valueOf("IN_PROGRESS"),
        )

        assertEquals(
            TemporalState.ENDED,
            TemporalState.valueOf("ENDED"),
        )

        assertEquals(
            TemporalState.DUE,
            TemporalState.valueOf("DUE"),
        )

        assertEquals(
            TemporalState.OVERDUE,
            TemporalState.valueOf("OVERDUE"),
        )
    }
}
