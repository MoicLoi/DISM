package me.moiclathy.dism.external.schedule.source

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExternalScheduleSourceIdTest {

    @Test
    fun validIdShouldBeAccepted() {

        val id =
            ExternalScheduleSourceId(
                "xiqueer-main"
            )

        assertEquals(
            "xiqueer-main",
            id.value,
        )

        assertEquals(
            "xiqueer-main",
            id.toString(),
        )
    }

    @Test
    fun blankIdShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleSourceId(
                " "
            )
        }
    }
}
