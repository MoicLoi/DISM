package me.moiclathy.dism.external.schedule.recurrence

import kotlinx.datetime.DayOfWeek
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CourseRecurrenceRuleTest {

    @Test
    fun weeklyRuleShouldBeCreated() {

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                interval =
                    1,

                count =
                    16,

                byDay =
                    listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.WEDNESDAY,
                    ),
            )

        assertEquals(
            RecurrenceFrequency.WEEKLY,
            rule.frequency,
        )

        assertEquals(
            16,
            rule.count,
        )

        assertEquals(
            true,
            rule.isFinite,
        )

        assertEquals(
            true,
            rule.hasDayFilter,
        )
    }

    @Test
    fun zeroIntervalShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                interval =
                    0,
            )
        }
    }

    @Test
    fun duplicateDaysShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                byDay =
                    listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.MONDAY,
                    ),
            )
        }
    }

    @Test
    fun countAndUntilShouldNotBeCombined() {

        assertFailsWith<IllegalArgumentException> {

            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                count =
                    16,

                until =
                    kotlin.time.Instant.parse(
                        "2026-12-31T00:00:00Z"
                    ),
            )
        }
    }
}
