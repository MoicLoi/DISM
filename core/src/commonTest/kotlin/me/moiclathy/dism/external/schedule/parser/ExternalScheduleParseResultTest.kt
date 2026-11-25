package me.moiclathy.dism.external.schedule.parser

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ExternalScheduleParseResultTest {

    private val schedule =
        ExternalSchedule(
            id =
                ExternalScheduleId(
                    "schedule-001"
                ),

            source =
                ExternalScheduleOrigin.FILE,

            name =
                "测试课程表",

            importedAt =
                Instant.parse(
                    "2026-09-16T08:00:00Z"
                ),

            entries =
                emptyList(),
        )

    @Test
    fun successShouldContainSchedule() {

        val result =
            ExternalScheduleParseResult.success(
                schedule =
                    schedule,

                warnings =
                    listOf(
                        "Course code missing."
                    ),
            )

        assertTrue(
            result.success
        )

        assertTrue(
            result.hasWarnings
        )

        assertEquals(
            schedule,
            result.schedule,
        )

        assertTrue(
            result.errors.isEmpty()
        )
    }

    @Test
    fun failureShouldContainErrors() {

        val result =
            ExternalScheduleParseResult.failure(
                ExternalScheduleParseError(
                    code =
                        ExternalScheduleParseErrorCode.INVALID_TIME,

                    message =
                        "Invalid time.",
                )
            )

        assertFalse(
            result.success
        )

        assertNull(
            result.schedule
        )

        assertEquals(
            1,
            result.errors.size,
        )
    }

    @Test
    fun multipleErrorsShouldBeSupported() {

        val result =
            ExternalScheduleParseResult.failure(
                errors =
                    listOf(
                        ExternalScheduleParseError(
                            code =
                                ExternalScheduleParseErrorCode.INVALID_TIME,

                            message =
                                "Invalid start time.",
                        ),

                        ExternalScheduleParseError(
                            code =
                                ExternalScheduleParseErrorCode.MISSING_FIELD,

                            message =
                                "Missing title.",
                        ),
                    )
            )

        assertEquals(
            2,
            result.errors.size,
        )

        assertFalse(
            result.success
        )
    }
}
