package me.moiclathy.dism.event.duration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.minutes

class EventDurationTest {

    @Test
    fun fixedDurationShouldBeStored() {

        val duration =
            EventDuration(
                value =
                    90.minutes,

                type =
                    EventDurationType.FIXED,
            )

        assertEquals(
            90.minutes,
            duration.value,
        )

        assertEquals(
            EventDurationType.FIXED,
            duration.type,
        )
    }

    @Test
    fun negativeDurationShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            EventDuration(
                value =
                    (-5).minutes,
            )
        }
    }

    @Test
    fun zeroDurationShouldBeAllowedByDefault() {

        val duration =
            EventDuration.zero()

        assertEquals(
            0.minutes,
            duration.value,
        )
    }

    @Test
    fun estimateShouldRequireOrderedBounds() {

        val estimate =
            EventDurationEstimate(
                minimum =
                    5.minutes,

                expected =
                    10.minutes,

                maximum =
                    15.minutes,
            )

        assertEquals(
            5.minutes,
            estimate.minimum,
        )

        assertEquals(
            10.minutes,
            estimate.expected,
        )

        assertEquals(
            15.minutes,
            estimate.maximum,
        )
    }

    @Test
    fun invalidEstimateShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            EventDurationEstimate(
                minimum =
                    10.minutes,

                expected =
                    5.minutes,

                maximum =
                    15.minutes,
            )
        }
    }

    @Test
    fun exactEstimateShouldBeExact() {

        val estimate =
            EventDurationEstimate(
                minimum =
                    10.minutes,

                expected =
                    10.minutes,

                maximum =
                    10.minutes,
            )

        assertTrue(
            estimate.isExact
        )
    }

    @Test
    fun nonExactEstimateShouldNotBeExact() {

        val estimate =
            EventDurationEstimate(
                minimum =
                    5.minutes,

                expected =
                    10.minutes,

                maximum =
                    15.minutes,
            )

        assertTrue(
            !estimate.isExact
        )
    }
}
