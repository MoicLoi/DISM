package me.moiclathy.dism.external.schedule.parser

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class ExternalScheduleParseErrorTest {

    @Test
    fun errorShouldStoreFields() {

        val error =
            ExternalScheduleParseError(
                code =
                    ExternalScheduleParseErrorCode.INVALID_TIME,

                message =
                    "Unable to parse start time.",

                field =
                    "startAt",
            )

        assertEquals(
            ExternalScheduleParseErrorCode.INVALID_TIME,
            error.code,
        )

        assertEquals(
            "Unable to parse start time.",
            error.message,
        )

        assertEquals(
            "startAt",
            error.field,
        )
    }

    @Test
    fun blankMessageShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleParseError(
                code =
                    ExternalScheduleParseErrorCode.MALFORMED_DATA,

                message =
                    " ",
            )
        }
    }

    @Test
    fun blankFieldShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            ExternalScheduleParseError(
                code =
                    ExternalScheduleParseErrorCode.INVALID_ENTRY,

                message =
                    "Invalid entry.",

                field =
                    " ",
            )
        }
    }
}
