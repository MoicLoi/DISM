package me.moiclathy.dism.external.schedule

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExternalScheduleIdTest {

    @Test
    fun validIdShouldBeAccepted() {

        val id =
            ExternalScheduleId(
                "xiqueer-course-001"
            )

        assertEquals(
            "xiqueer-course-001",
            id.value,
        )

        assertEquals(
            "xiqueer-course-001",
            id.toString(),
        )
    }

    @Test
    fun blankIdShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleId(
                " "
            )
        }
    }

    @Test
    fun equalIdsShouldBeEqual() {

        assertEquals(
            ExternalScheduleId("a"),
            ExternalScheduleId("a"),
        )
    }
}
