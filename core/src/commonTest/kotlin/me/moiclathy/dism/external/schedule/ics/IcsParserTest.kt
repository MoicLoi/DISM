package me.moiclathy.dism.external.schedule.ics

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class IcsParserTest {

    private val ics =
        """
        BEGIN:VCALENDAR
        VERSION:2.0
        PRODID:-//DISM//Test//EN
        X-WR-CALNAME:测试课程表
        BEGIN:VEVENT
        UID:event-001
        SUMMARY:高等数学
        DTSTART:20260916T080000Z
        DTEND:20260916T094000Z
        LOCATION:A301
        END:VEVENT
        END:VCALENDAR
        """.trimIndent()

    @Test
    fun parserShouldParseCalendar() {

        val result =
            IcsParser()
                .parse(
                    ics
                )
                .getOrThrow()

        assertEquals(
            1,
            result.eventCount,
        )

        assertEquals(
            "2.0",
            result.version,
        )

        assertEquals(
            "测试课程表",
            result.calendarName,
        )
    }

    @Test
    fun parserShouldParseEventProperties() {

        val result =
            IcsParser()
                .parse(
                    ics
                )
                .getOrThrow()

        val event =
            result.events.first()

        assertEquals(
            "event-001",
            event
                .first {
                    it.name == "UID"
                }
                .value,
        )

        assertEquals(
            "高等数学",
            event
                .first {
                    it.name == "SUMMARY"
                }
                .value,
        )
    }

    @Test
    fun foldedLinesShouldBeUnfolded() {

        val content =
            """
            BEGIN:VCALENDAR
            VERSION:2.0
            BEGIN:VEVENT
            UID:event-001
            SUMMARY:这是一个很长的
             课程名称
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        val event =
            IcsParser()
                .parse(
                    content
                )
                .getOrThrow()
                .events
                .first()

        val summary =
            event
                .first {
                    it.name == "SUMMARY"
                }

        assertEquals(
            "这是一个很长的课程名称",
            summary.value,
        )
    }

    @Test
    fun missingVersionShouldFailByDefault() {

        val content =
            """
            BEGIN:VCALENDAR
            BEGIN:VEVENT
            UID:event-001
            SUMMARY:测试
            END:VEVENT
            END:VCALENDAR
            """.trimIndent()

        val result =
            IcsParser()
                .parse(
                    content
                )

        assertTrue(
            result.isFailure
        )
    }
}
