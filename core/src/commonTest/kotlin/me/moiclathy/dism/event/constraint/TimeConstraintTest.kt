package me.moiclathy.dism.event.constraint

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TimeConstraintTest {

    private val start =
        Instant.parse(
            "2026-09-16T10:00:00Z",
        )

    private val end =
        Instant.parse(
            "2026-09-16T11:00:00Z",
        )

    @Test
    fun earliestStartShouldStoreStart() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.EARLIEST_START,

                start =
                    start,
            )

        assertEquals(
            start,
            constraint.start,
        )
    }

    @Test
    fun latestStartShouldStoreStart() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_START,

                start =
                    start,
            )

        assertEquals(
            start,
            constraint.start,
        )
    }

    @Test
    fun latestEndShouldStoreEnd() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_END,

                end =
                    end,
            )

        assertEquals(
            end,
            constraint.end,
        )
    }

    @Test
    fun fixedTimeShouldStoreStartOnly() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.FIXED_TIME,

                start =
                    start,
            )

        assertEquals(
            start,
            constraint.start,
        )
    }

    @Test
    fun timeWindowShouldStoreBothBounds() {

        val constraint =
            TimeConstraint(
                type =
                    TimeConstraintType.TIME_WINDOW,

                start =
                    start,

                end =
                    end,
            )

        assertEquals(
            start,
            constraint.start,
        )

        assertEquals(
            end,
            constraint.end,
        )
    }

    @Test
    fun earliestStartWithoutStartShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            TimeConstraint(
                type =
                    TimeConstraintType.EARLIEST_START,
            )
        }
    }

    @Test
    fun latestEndWithoutEndShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            TimeConstraint(
                type =
                    TimeConstraintType.LATEST_END,
            )
        }
    }

    @Test
    fun fixedTimeShouldNotHaveEnd() {

        assertFailsWith<IllegalArgumentException> {

            TimeConstraint(
                type =
                    TimeConstraintType.FIXED_TIME,

                start =
                    start,

                end =
                    end,
            )
        }
    }

    @Test
    fun invalidWindowShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            TimeConstraint(
                type =
                    TimeConstraintType.TIME_WINDOW,

                start =
                    end,

                end =
                    start,
            )
        }
    }

    @Test
    fun blankReasonShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            TimeConstraint(
                type =
                    TimeConstraintType.FIXED_TIME,

                start =
                    start,

                reason =
                    " ",
            )
        }
    }
}
