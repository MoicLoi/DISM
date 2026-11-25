package me.moiclathy.dism.event.state

import kotlin.test.Test
import kotlin.test.assertEquals

class EventStateTest {

    @Test
    fun eventStatesShouldExist() {

        assertEquals(
            EventState.PLANNED,
            EventState.valueOf("PLANNED"),
        )

        assertEquals(
            EventState.ACTIVE,
            EventState.valueOf("ACTIVE"),
        )

        assertEquals(
            EventState.COMPLETED,
            EventState.valueOf("COMPLETED"),
        )

        assertEquals(
            EventState.CANCELLED,
            EventState.valueOf("CANCELLED"),
        )

        assertEquals(
            EventState.EXPIRED,
            EventState.valueOf("EXPIRED"),
        )

        assertEquals(
            EventState.POSTPONED,
            EventState.valueOf("POSTPONED"),
        )
    }
}
