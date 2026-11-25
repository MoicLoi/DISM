package me.moiclathy.dism.external.schedule.recurrence

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class CourseRecurrenceExceptionTest {

    private val instant =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    @Test
    fun excludedOccurrenceShouldStoreTime() {

        val exception =
            CourseRecurrenceException.Excluded(
                occurrenceAt =
                    instant,
            )

        assertEquals(
            instant,
            exception.occurrenceAt,
        )
    }

    @Test
    fun addedOccurrenceShouldStoreTime() {

        val exception =
            CourseRecurrenceException.Added(
                occurrenceAt =
                    instant,
            )

        assertEquals(
            instant,
            exception.occurrenceAt,
        )
    }
}
