package me.moiclathy.dism.event.slack

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration

class EventSlackTest {

    @Test
    fun positiveSlackShouldBePositive() {

        val slack =
            EventSlack(
                Duration.parse("20m")
            )

        assertTrue(slack.isPositive)
        assertFalse(slack.isZero)
        assertFalse(slack.isNegative)
    }

    @Test
    fun zeroSlackShouldBeZero() {

        val slack =
            EventSlack(
                Duration.ZERO
            )

        assertFalse(slack.isPositive)
        assertTrue(slack.isZero)
        assertFalse(slack.isNegative)
    }

    @Test
    fun negativeSlackShouldBeNegative() {

        val slack =
            EventSlack(
                Duration.parse("-5m")
            )

        assertFalse(slack.isPositive)
        assertFalse(slack.isZero)
        assertTrue(slack.isNegative)
    }

    @Test
    fun policyShouldRejectNegativeThreshold() {

        kotlin.test.assertFailsWith<IllegalArgumentException> {

            EventSlackPolicy(
                criticalThreshold =
                    Duration.parse("-1m")
            )
        }
    }

    @Test
    fun unknownEvaluationShouldNotHaveSlack() {

        val evaluation =
            EventSlackEvaluation(
                slack = null,
                status = EventSlackStatus.UNKNOWN,
                evaluatedAt =
                    Instant.parse(
                        "2026-09-16T10:00:00Z"
                    ),
            )

        assertEquals(
            EventSlackStatus.UNKNOWN,
            evaluation.status,
        )

        assertEquals(
            null,
            evaluation.slack,
        )
    }

    @Test
    fun knownEvaluationShouldHaveSlack() {

        val evaluation =
            EventSlackEvaluation(
                slack =
                    EventSlack(
                        Duration.parse("10m")
                    ),

                status =
                    EventSlackStatus.ZERO,

                evaluatedAt =
                    Instant.parse(
                        "2026-09-16T10:00:00Z"
                    ),
            )

        assertEquals(
            Duration.parse("10m"),
            evaluation.slack!!.duration,
        )
    }
}
