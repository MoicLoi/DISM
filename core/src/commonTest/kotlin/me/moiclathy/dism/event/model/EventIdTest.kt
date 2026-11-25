package me.moiclathy.dism.event.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class EventIdTest {

    @Test
    fun eventIdShouldPreserveValue() {

        val id =
            EventId(
                "event:experiment-class:001",
            )

        assertEquals(
            "event:experiment-class:001",
            id.value,
        )

        assertEquals(
            "event:experiment-class:001",
            id.toString(),
        )
    }

    @Test
    fun blankEventIdShouldBeRejected() {

        assertFailsWith<IllegalArgumentException> {
            EventId("")
        }
    }

    @Test
    fun whitespaceEventIdShouldBeRejected() {

        assertFailsWith<IllegalArgumentException> {
            EventId("   ")
        }
    }

    @Test
    fun equalValuesShouldProduceEqualIds() {

        val first =
            EventId("event:test")

        val second =
            EventId("event:test")

        assertEquals(
            first,
            second,
        )
    }
}
