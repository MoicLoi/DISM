package me.moiclathy.dism.external.schedule.ics

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleOrigin
import me.moiclathy.dism.external.schedule.parser.ExternalScheduleParseInput
import me.moiclathy.dism.external.schedule.source.ExternalScheduleSourceId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class IcsScheduleParserTest {

    private val importedAt =
        Instant.parse(
            "2026-09-16T07:00:00Z"
        )

    private val ics =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//DISM//Test//EN
        X-WR-CALNAME:2026秋季课程表
        BEGIN:VEVENT
        UID:course-001
        SUMMARY:高等数学
        DTSTART:20260916T080000Z
        DTEND:20260916T094000Z
        LOCATION:A教学楼301
        ORGANIZER:张老师
        X-COURSE-CODE:MATH101
        DESCRIPTION:第一周课程
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    private fun input(
        origin: ExternalScheduleOrigin =
            ExternalScheduleOrigin.ICS,
        content: String =
            ics,
    ) =
        ExternalScheduleParseInput(
            sourceId =
                ExternalScheduleSourceId(
                    "school-calendar"
                ),

            origin =
                origin,

            rawContent =
                content,

            metadata =
                mapOf(
                    "importedAt" to
                        importedAt.toString(),
                ),
        )

    @Test
    fun parserShouldCreateExternalSchedule() {

        val result =
            IcsScheduleParser()
                .parse(
                    input()
                )

        assertTrue(
            result.success
        )

        val schedule =
            result.schedule

        assertNotNull(
            schedule
        )

        assertEquals(
            "2026秋季课程表",
            schedule.name,
        )

        assertEquals(
            ExternalScheduleOrigin.ICS,
            schedule.source,
        )

        assertEquals(
            1,
            schedule.size,
        )
    }

    @Test
    fun parserShouldCreateEntry() {

        val result =
            IcsScheduleParser()
                .parse(
                    input()
                )

        val entry =
            result
                .schedule!!
                .entries
                .first()

        assertEquals(
            "course-001",
            entry.id.value,
        )

        assertEquals(
            "高等数学",
            entry.title,
        )

        assertEquals(
            "A教学楼301",
            entry.location,
        )

        assertEquals(
            "张老师",
            entry.instructor,
        )

        assertEquals(
            "MATH101",
            entry.courseCode,
        )

        assertEquals(
            "MATH101",
            entry.metadata["X-COURSE-CODE"],
        )
    }

    @Test
    fun parserShouldRejectWrongOrigin() {

        val result =
            IcsScheduleParser()
                .parse(
                    input(
                        origin =
                            ExternalScheduleOrigin.FILE
                    )
                )

        assertFalse(
            result.success
        )

        assertEquals(
            "UNSUPPORTED_FORMAT",
            result.errors
                .first()
                .code
                .name,
        )
    }

    @Test
    fun parserShouldSupportMultipleEvents() {

        val content =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:event-001
            SUMMARY:高等数学
            DTSTART:20260916T080000Z
            DTEND:20260916T094000Z
            END:VEVENT
            BEGIN:VEVENT
            UID:event-002
            SUMMARY:英语
            DTSTART:20260917T100000Z
            DTEND:20260917T114000Z
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        val result =
            IcsScheduleParser()
                .parse(
                    input(
                        content =
                            content
                    )
                )

        assertTrue(
            result.success
        )

        assertEquals(
            2,
            result.schedule!!.size,
        )
    }

    @Test
    fun parserShouldSupportDateOnlyEvent() {

        val content =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:event-001
            SUMMARY:全天事件
            DTSTART;VALUE=DATE:20260916
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        val result =
            IcsScheduleParser()
                .parse(
                    input(
                        content =
                            content
                    )
                )

        assertTrue(
            result.success
        )

        val entry =
            result.schedule!!
                .entries
                .first()

        assertEquals(
            null,
            entry.startAt,
        )
    }
}
