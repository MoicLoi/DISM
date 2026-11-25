package me.moiclathy.dism.event.slack

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeterministicEventSlackEngineTest {

    private val engine =
        DeterministicEventSlackEngine()

    private val boundary =
        Instant.parse(
            "2026-09-16T11:30:00Z"
        )

    private val early =
        Instant.parse(
            "2026-09-16T10:00:00Z"
        )

    private val critical =
        Instant.parse(
            "2026-09-16T11:25:00Z"
        )

    private val exact =
        Instant.parse(
            "2026-09-16T11:30:00Z"
        )

    private val late =
        Instant.parse(
            "2026-09-16T11:35:00Z"
        )

    private val evaluatedAt =
        Instant.parse(
            "2026-09-16T09:00:00Z"
        )

    @Test
    fun largePositiveSlackShouldBeAvailable() {

        val result =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    early,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.AVAILABLE,
            result.status,
        )

        assertEquals(
            "1h 30m",
            result.slack!!.duration.toString(),
        )
    }

    @Test
    fun criticalSlackShouldBeCritical() {

        val result =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    critical,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.CRITICAL,
            result.status,
        )

        assertEquals(
            "5m",
            result.slack!!.duration.toString(),
        )
    }

    @Test
    fun exactBoundaryShouldBeZero() {

        val result =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    exact,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.ZERO,
            result.status,
        )

        assertTrue(
            result.slack!!.isZero
        )
    }

    @Test
    fun negativeSlackShouldBeOverdue() {

        val result =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    late,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.OVERDUE,
            result.status,
        )

        assertTrue(
            result.slack!!.isNegative
        )

        assertEquals(
            "-5m",
            result.slack!!.duration.toString(),
        )
    }

    @Test
    fun missingBoundaryShouldBeUnknown() {

        val result =
            engine.evaluate(
                boundary =
                    null,

                actual =
                    early,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.UNKNOWN,
            result.status,
        )

        assertNull(
            result.slack,
        )
    }

    @Test
    fun missingActualShouldBeUnknown() {

        val result =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    null,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.UNKNOWN,
            result.status,
        )

        assertNull(
            result.slack,
        )
    }

    @Test
    fun criticalThresholdShouldBeConfigurable() {

        val customEngine =
            DeterministicEventSlackEngine(
                policy =
                    EventSlackPolicy(
                        criticalThreshold =
                            kotlin.time.Duration.parse("20m")
                    )
            )

        val result =
            customEngine.evaluate(
                boundary =
                    boundary,

                actual =
                    Instant.parse(
                        "2026-09-16T11:15:00Z"
                    ),

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.CRITICAL,
            result.status,
        )
    }

    @Test
    fun exactCriticalThresholdShouldBeCritical() {

        val result =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    Instant.parse(
                        "2026-09-16T11:20:00Z"
                    ),

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventSlackStatus.CRITICAL,
            result.status,
        )
    }

    @Test
    fun resultShouldBeDeterministic() {

        val first =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    critical,

                evaluatedAt =
                    evaluatedAt,
            )

        val second =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    critical,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun evaluatedAtShouldBePreserved() {

        val result =
            engine.evaluate(
                boundary =
                    boundary,

                actual =
                    early,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            evaluatedAt,
            result.evaluatedAt,
        )
    }
}
