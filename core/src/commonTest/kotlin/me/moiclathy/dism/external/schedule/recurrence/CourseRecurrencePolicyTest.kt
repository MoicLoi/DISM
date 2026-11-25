package me.moiclathy.dism.external.schedule.recurrence

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class CourseRecurrencePolicyTest {

    @Test
    fun defaultPolicyShouldBeStable() {

        val policy =
            CourseRecurrencePolicy()

        assertEquals(
            128,
            policy.maxOccurrences,
        )

        assertEquals(
            true,
            policy.allowDaily,
        )

        assertEquals(
            true,
            policy.allowWeekly,
        )
    }

    @Test
    fun zeroMaxOccurrencesShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            CourseRecurrencePolicy(
                maxOccurrences = 0,
            )
        }
    }
}
