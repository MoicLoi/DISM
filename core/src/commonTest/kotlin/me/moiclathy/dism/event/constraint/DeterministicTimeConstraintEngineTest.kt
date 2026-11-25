package me.moiclathy.dism.event.constraint

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeterministicTimeConstraintEngineTest {

    private val engine =
        DeterministicTimeConstraintEngine()

    private val earlier =
        Instant.parse(
            "2026-09-16T09:30:00Z",
        )

    private val start =
        Instant.parse(
            "2026-09-16T10:00:00Z",
        )

    private val middle =
        Instant.parse(
            "2026-09-16T10:30:00Z",
        )

    private val end =
        Instant.parse(
            "2026-09-16T11:00:00Z",
        )

    private val later =
        Instant.parse(
            "2026-09-16T11:30:00Z",
        )

    @Test
    fun earliestStartShouldAcceptBoundary() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.EARLIEST_START,

                start =
                    start,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertTrue(result.satisfied)

        assertTrue(
            result.violations.isEmpty()
        )
    }

    @Test
    fun earliestStartShouldRejectTooEarly() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.EARLIEST_START,

                start =
                    start,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    earlier,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)

        assertEquals(
            1,
            result.violations.size,
        )
    }

    @Test
    fun latestStartShouldAcceptBoundary() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_START,

                start =
                    start,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertTrue(result.satisfied)
    }

    @Test
    fun latestStartShouldRejectTooLate() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_START,

                start =
                    start,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    middle,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)
    }

    @Test
    fun latestEndShouldAcceptBoundary() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_END,

                end =
                    end,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertTrue(result.satisfied)
    }

    @Test
    fun latestEndShouldRejectTooLate() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_END,

                end =
                    end,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    later,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)

        assertEquals(
            "Event ended too late.",
            result.violations.first().message,
        )
    }

    @Test
    fun fixedTimeShouldAcceptExactTime() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.FIXED_TIME,

                start =
                    start,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertTrue(result.satisfied)
    }

    @Test
    fun fixedTimeShouldRejectDifferentTime() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.FIXED_TIME,

                start =
                    start,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    middle,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)
    }

    @Test
    fun timeWindowShouldAcceptInsideWindow() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.TIME_WINDOW,

                start =
                    start,

                end =
                    end,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    middle,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertTrue(result.satisfied)
    }

    @Test
    fun timeWindowShouldRejectBeforeWindow() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.TIME_WINDOW,

                start =
                    start,

                end =
                    end,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    earlier,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)
    }

    @Test
    fun timeWindowShouldRejectAfterWindow() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.TIME_WINDOW,

                start =
                    start,

                end =
                    end,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    later,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)
    }

    @Test
    fun multipleConstraintsShouldAllBeEvaluated() {

        val constraints =
            listOf(
                TimeConstraint(
                    type =
                        TimeConstraintType.EARLIEST_START,

                    start =
                        start,
                ),
                TimeConstraint(
                    type =
                        TimeConstraintType.LATEST_END,

                    end =
                        end,
                ),
            )

        val result =
            engine.evaluateAll(
                constraints =
                    constraints,

                actualStart =
                    earlier,

                actualEnd =
                    later,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)

        assertEquals(
            2,
            result.violations.size,
        )
    }

    @Test
    fun missingActualStartShouldProduceViolation() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_START,

                start =
                    start,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    null,

                actualEnd =
                    end,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)

        assertEquals(
            "Actual start time is missing.",
            result.violations.first().message,
        )
    }

    @Test
    fun missingActualEndShouldProduceViolation() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_END,

                end =
                    end,
            )

        val result =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    null,

                evaluatedAt =
                    middle,
            )

        assertFalse(result.satisfied)

        assertEquals(
            "Actual end time is missing.",
            result.violations.first().message,
        )
    }

    @Test
    fun evaluationShouldBeDeterministic() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_END,

                end =
                    end,
            )

        val first =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    later,

                evaluatedAt =
                    middle,
            )

        val second =
            engine.evaluate(
                constraint =
                    constraint,

                actualStart =
                    start,

                actualEnd =
                    later,

                evaluatedAt =
                    middle,
            )

        assertEquals(
            first,
            second,
        )
    }
}
