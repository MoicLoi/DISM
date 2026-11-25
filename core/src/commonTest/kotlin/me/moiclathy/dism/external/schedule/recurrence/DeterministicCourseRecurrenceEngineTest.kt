package me.moiclathy.dism.external.schedule.recurrence

import kotlinx.datetime.DayOfWeek
import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DeterministicCourseRecurrenceEngineTest {

    private val entryId =
        ExternalScheduleId(
            "course-001"
        )

    private val start =
        Instant.parse(
            "2026-09-14T08:00:00Z"
        )

    private val engine =
        DeterministicCourseRecurrenceEngine(
            CourseRecurrencePolicy(
                maxOccurrences = 32,
            )
        )

    @Test
    fun weeklyCountShouldExpand() {

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                interval =
                    1,

                count =
                    4,
            )

        val result =
            engine.expand(
                sourceEntryId =
                    entryId,

                startAt =
                    start,

                rule =
                    rule,
            )

        assertTrue(
            result.success
        )

        assertEquals(
            4,
            result.expansion!!.size,
        )
    }

    @Test
    fun weeklyIntervalShouldBeRespected() {

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                interval =
                    2,

                count =
                    3,
            )

        val result =
            engine.expand(
                sourceEntryId =
                    entryId,

                startAt =
                    start,

                rule =
                    rule,
            )

        val instances =
            result
                .expansion!!
                .instances

        assertEquals(
            3,
            instances.size,
        )

        assertEquals(
            "2026-09-14T08:00:00Z",
            instances[0]
                .occurrenceAt
                .toString(),
        )

        assertEquals(
            "2026-09-28T08:00:00Z",
            instances[1]
                .occurrenceAt
                .toString(),
        )

        assertEquals(
            "2026-10-12T08:00:00Z",
            instances[2]
                .occurrenceAt
                .toString(),
        )
    }

    @Test
    fun excludedDateShouldNotBeGenerated() {

        val excluded =
            start.plus(
                kotlin.time.Duration.parse(
                    "7d"
                )
            )

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                count =
                    4,
            )

        val result =
            engine.expand(
                sourceEntryId =
                    entryId,

                startAt =
                    start,

                rule =
                    rule,

                exceptions =
                    listOf(
                        CourseRecurrenceException.Excluded(
                            excluded
                        )
                    ),
            )

        assertTrue(
            result.success
        )

        assertEquals(
            3,
            result.expansion!!.size,
        )

        assertTrue(
            result.expansion
                .instances
                .none {
                    it.occurrenceAt == excluded
                }
        )
    }

    @Test
    fun addedDateShouldBeIncluded() {

        val added =
            Instant.parse(
                "2026-09-18T08:00:00Z"
            )

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                count =
                    2,
            )

        val result =
            engine.expand(
                sourceEntryId =
                    entryId,

                startAt =
                    start,

                rule =
                    rule,

                exceptions =
                    listOf(
                        CourseRecurrenceException.Added(
                            added
                        )
                    ),
            )

        assertTrue(
            result.success
        )

        assertTrue(
            result.expansion!!
                .instances
                .any {
                    it.occurrenceAt == added
                }
        )
    }

    @Test
    fun byDayShouldExpandMultipleDaysPerWeek() {

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                count =
                    6,

                byDay =
                    listOf(
                        DayOfWeek.MONDAY,
                        DayOfWeek.WEDNESDAY,
                    ),
            )

        val result =
            engine.expand(
                sourceEntryId =
                    entryId,

                startAt =
                    start,

                rule =
                    rule,
            )

        assertTrue(
            result.success
        )

        assertEquals(
            6,
            result.expansion!!.size,
        )
    }

    @Test
    fun expansionShouldBeDeterministic() {

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                count =
                    8,
            )

        val first =
            engine.expand(
                entryId,
                start,
                rule,
            )

        val second =
            engine.expand(
                entryId,
                start,
                rule,
            )

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun instanceIdShouldContainSourceIdentity() {

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.WEEKLY,

                count =
                    1,
            )

        val result =
            engine.expand(
                entryId,
                start,
                rule,
            )

        val instance =
            result
                .expansion!!
                .instances
                .first()

        assertTrue(
            instance.instanceId
                .startsWith(
                    "course-001@"
                )
        )
    }

    @Test
    fun unsupportedHighFrequencyShouldFail() {

        val rule =
            CourseRecurrenceRule(
                frequency =
                    RecurrenceFrequency.HOURLY,

                count =
                    10,
            )

        val result =
            engine.expand(
                entryId,
                start,
                rule,
            )

        assertTrue(
            result.errors.isNotEmpty()
        )
    }
}
