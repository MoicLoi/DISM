package me.moiclathy.dism.external.schedule.recurrence

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CourseRecurrenceRuleParserTest {

    private val parser =
        CourseRecurrenceRuleParser()

    @Test
    fun weeklyCountRuleShouldParse() {

        val result =
            parser.parse(
                "FREQ=WEEKLY;INTERVAL=1;COUNT=16;BYDAY=MO,WE"
            )

        assertTrue(
            result.isSuccess
        )

        val rule =
            result.getOrThrow()

        assertEquals(
            RecurrenceFrequency.WEEKLY,
            rule.frequency,
        )

        assertEquals(
            1,
            rule.interval,
        )

        assertEquals(
            16,
            rule.count,
        )

        assertEquals(
            listOf(
                DayOfWeek.MONDAY,
                DayOfWeek.WEDNESDAY,
            ),
            rule.byDay,
        )
    }

    @Test
    fun weeklyUntilRuleShouldParse() {

        val result =
            parser.parse(
                "RRULE:FREQ=WEEKLY;UNTIL=20261231T000000Z"
            )

        assertTrue(
            result.isSuccess
        )

        assertEquals(
            "2026-12-31T00:00:00Z",
            result
                .getOrThrow()
                .until
                .toString(),
        )
    }

    @Test
    fun invalidFrequencyShouldFail() {

        val result =
            parser.parse(
                "FREQ=INVALID"
            )

        assertTrue(
            result.isFailure
        )
    }

    @Test
    fun missingFrequencyShouldFail() {

        val result =
            parser.parse(
                "COUNT=16"
            )

        assertTrue(
            result.isFailure
        )
    }

    @Test
    fun duplicateComponentShouldFail() {

        val result =
            parser.parse(
                "FREQ=WEEKLY;FREQ=DAILY"
            )

        assertTrue(
            result.isFailure
        )
    }
}
