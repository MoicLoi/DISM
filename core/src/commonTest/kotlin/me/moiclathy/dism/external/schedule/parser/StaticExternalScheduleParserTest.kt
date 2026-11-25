package me.moiclathy.dism.external.schedule.parser

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalSchedule
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.source.ExternalScheduleSourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class StaticExternalScheduleParserTest {

    private val time =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

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
                time,

            entries =
                emptyList(),
        )

    @Test
    fun parserShouldReturnSchedule() {

        val parser =
            StaticExternalScheduleParser(
                schedule =
                    schedule,
            )

        val input =
            ExternalScheduleParseInput(
                sourceId =
                    ExternalScheduleSourceId(
                        "file-main"
                    ),

                origin =
                    ExternalScheduleOrigin.FILE,

                rawContent =
                    "fixture",
            )

        val result =
            parser.parse(input)

        assertTrue(
            result.success
        )

        assertEquals(
            schedule,
            result.schedule,
        )
    }

    @Test
    fun parserShouldRejectWrongOrigin() {

        val parser =
            StaticExternalScheduleParser(
                schedule =
                    schedule,
            )

        val input =
            ExternalScheduleParseInput(
                sourceId =
                    ExternalScheduleSourceId(
                        "xiqueer-main"
                    ),

                origin =
                    ExternalScheduleOrigin.XIQUEER,

                rawContent =
                    "fixture",
            )

        val result =
            parser.parse(input)

        assertFalse(
            result.success
        )

        assertEquals(
            ExternalScheduleParseErrorCode.UNSUPPORTED_FORMAT,
            result.errors.first().code,
        )
    }

    @Test
    fun parserShouldBeDeterministic() {

        val parser =
            StaticExternalScheduleParser(
                schedule =
                    schedule,
            )

        val input =
            ExternalScheduleParseInput(
                sourceId =
                    ExternalScheduleSourceId(
                        "file-main"
                    ),

                origin =
                    ExternalScheduleOrigin.FILE,

                rawContent =
                    "fixture",
            )

        val first =
            parser.parse(input)

        val second =
            parser.parse(input)

        assertEquals(
            first,
            second,
        )
    }
}
