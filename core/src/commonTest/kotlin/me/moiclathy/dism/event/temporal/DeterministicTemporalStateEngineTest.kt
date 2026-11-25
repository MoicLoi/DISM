package me.moiclathy.dism.event.temporal

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import me.moiclathy.dism.event.model.EventTemporal

class DeterministicTemporalStateEngineTest {

    private val engine =
        DeterministicTemporalStateEngine()

    private val before =
        Instant.parse(
            "2026-09-16T09:00:00Z",
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

    private val after =
        Instant.parse(
            "2026-09-16T12:00:00Z",
        )

    @Test
    fun unscheduledShouldBeUnscheduled() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Unscheduled,

                evaluatedAt =
                    middle,
            )

        assertEquals(
            TemporalState.UNSCHEDULED,
            result.state,
        )
    }

    @Test
    fun pointBeforeStartShouldBeUpcoming() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.At(
                        start,
                    ),

                evaluatedAt =
                    before,
            )

        assertEquals(
            TemporalState.UPCOMING,
            result.state,
        )
    }

    @Test
    fun pointAtStartShouldBeEnded() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.At(
                        start,
                    ),

                evaluatedAt =
                    start,
            )

        assertEquals(
            TemporalState.ENDED,
            result.state,
        )
    }

    @Test
    fun pointAfterStartShouldBeEnded() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.At(
                        start,
                    ),

                evaluatedAt =
                    after,
            )

        assertEquals(
            TemporalState.ENDED,
            result.state,
        )
    }

    @Test
    fun intervalBeforeStartShouldBeUpcoming() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Interval(
                        start =
                            start,

                        end =
                            end,
                    ),

                evaluatedAt =
                    before,
            )

        assertEquals(
            TemporalState.UPCOMING,
            result.state,
        )
    }

    @Test
    fun intervalAtStartShouldBeInProgress() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Interval(
                        start =
                            start,

                        end =
                            end,
                    ),

                evaluatedAt =
                    start,
            )

        assertEquals(
            TemporalState.IN_PROGRESS,
            result.state,
        )
    }

    @Test
    fun intervalMiddleShouldBeInProgress() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Interval(
                        start =
                            start,

                        end =
                            end,
                    ),

                evaluatedAt =
                    middle,
            )

        assertEquals(
            TemporalState.IN_PROGRESS,
            result.state,
        )
    }

    @Test
    fun intervalAtEndShouldBeEnded() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Interval(
                        start =
                            start,

                        end =
                            end,
                    ),

                evaluatedAt =
                    end,
            )

        assertEquals(
            TemporalState.ENDED,
            result.state,
        )
    }

    @Test
    fun intervalAfterEndShouldBeEnded() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Interval(
                        start =
                            start,

                        end =
                            end,
                    ),

                evaluatedAt =
                    after,
            )

        assertEquals(
            TemporalState.ENDED,
            result.state,
        )
    }

    @Test
    fun deadlineBeforeShouldBeUpcoming() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Deadline(
                        deadline =
                            end,
                    ),

                evaluatedAt =
                    middle,
            )

        assertEquals(
            TemporalState.UPCOMING,
            result.state,
        )
    }

    @Test
    fun deadlineAtShouldBeDue() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Deadline(
                        deadline =
                            end,
                    ),

                evaluatedAt =
                    end,
            )

        assertEquals(
            TemporalState.DUE,
            result.state,
        )
    }

    @Test
    fun deadlineAfterShouldBeOverdue() {

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Deadline(
                        deadline =
                            end,
                    ),

                evaluatedAt =
                    after,
            )

        assertEquals(
            TemporalState.OVERDUE,
            result.state,
        )
    }

    @Test
    fun deadlineAtCanBeOverdueByPolicy() {

        val engine =
            DeterministicTemporalStateEngine(
                TemporalStatePolicy(
                    deadlineAtIsDue = false,
                ),
            )

        val result =
            engine.evaluate(
                temporal =
                    EventTemporal.Deadline(
                        deadline =
                            end,
                    ),

                evaluatedAt =
                    end,
            )

        assertEquals(
            TemporalState.OVERDUE,
            result.state,
        )
    }

    @Test
    fun evaluationShouldBeDeterministic() {

        val temporal =
            EventTemporal.Interval(
                start =
                    start,

                end =
                    end,
            )

        val first =
            engine.evaluate(
                temporal =
                    temporal,

                evaluatedAt =
                    middle,
            )

        val second =
            engine.evaluate(
                temporal =
                    temporal,

                evaluatedAt =
                    middle,
            )

        assertEquals(
            first,
            second,
        )
    }
}
