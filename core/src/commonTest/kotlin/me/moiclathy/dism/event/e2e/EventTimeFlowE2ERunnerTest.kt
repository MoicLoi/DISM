package me.moiclathy.dism.event.e2e

import kotlin.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.event.alarm.DeterministicDynamicAlarmEngine
import me.moiclathy.dism.event.constraint.DeterministicTimeConstraintEngine
import me.moiclathy.dism.event.constraint.TimeConstraint
import me.moiclathy.dism.event.constraint.TimeConstraintType
import me.moiclathy.dism.event.duration.DeterministicEventDurationEngine
import me.moiclathy.dism.event.duration.EventDuration
import me.moiclathy.dism.event.model.Event
import me.moiclathy.dism.event.model.EventId
import me.moiclathy.dism.event.model.EventSource
import me.moiclathy.dism.event.model.EventTemporal
import me.moiclathy.dism.event.model.EventType
import me.moiclathy.dism.event.runtime.DeterministicEventTimeRuntime
import me.moiclathy.dism.event.runtime.DeterministicEventTimeRuntimeRecovery
import me.moiclathy.dism.event.runtime.EventTimeRuntimePersistenceEvent
import me.moiclathy.dism.event.runtime.EventTimeRuntimeState
import me.moiclathy.dism.event.runtime.InMemoryEventTimeRuntimePersistence
import me.moiclathy.dism.event.slack.DeterministicEventSlackEngine
import me.moiclathy.dism.event.temporal.DeterministicTemporalStateEngine

class EventTimeFlowE2ERunnerTest {

    private lateinit var persistence:
        InMemoryEventTimeRuntimePersistence

    private lateinit var runner:
        EventTimeFlowE2ERunner

    private val evaluatedAt =
        Instant.parse(
            "2026-09-16T07:30:00Z"
        )

    @BeforeTest
    fun setup() {

        persistence =
            InMemoryEventTimeRuntimePersistence()

        val runtime =
            DeterministicEventTimeRuntime()

        val recovery =
            DeterministicEventTimeRuntimeRecovery(
                persistence
            )

        runner =
            EventTimeFlowE2ERunner(
                temporalEngine =
                    DeterministicTemporalStateEngine(),

                durationEngine =
                    DeterministicEventDurationEngine(),

                constraintEngine =
                    DeterministicTimeConstraintEngine(),

                slackEngine =
                    DeterministicEventSlackEngine(),

                alarmEngine =
                    DeterministicDynamicAlarmEngine(),

                runtime =
                    runtime,

                persistence =
                    persistence,

                recovery =
                    recovery,
            )
    }

    private fun createEvent():
        Event {

        return Event(
            id =
                EventId(
                    "event-breakfast"
                ),

            title =
                "Breakfast",

            type =
                EventType.ONE_TIME,

            temporal =
                EventTemporal.At(
                    Instant.parse(
                        "2026-09-16T08:00:00Z"
                    )
                ),

            source =
                EventSource.USER_INPUT,
        )
    }

    @Test
    fun fullTimeFlowShouldSatisfyContract() {

        val event =
            createEvent()

        val result =
            runner.run(
                event =
                    event,

                duration =
                    EventDuration(
                        value =
                            kotlin.time.Duration.parse(
                                "10m"
                            )
                    ),

                constraint =
                    TimeConstraint(
                        type =
                            TimeConstraintType.LATEST_START,

                        start =
                            Instant.parse(
                                "2026-09-16T08:00:00Z"
                            ),
                    ),

                actualStart =
                    Instant.parse(
                        "2026-09-16T07:50:00Z"
                    ),

                actualEnd =
                    null,

                evaluatedAt =
                    evaluatedAt,
            )

        assertTrue(
            result.contractSatisfied,
            result.errors.joinToString()
        )

        assertTrue(
            result.successful
        )

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            result.trace.runtimeState,
        )

        assertTrue(
            result.trace.persistedEventCount > 0
        )

        assertTrue(
            result.trace.recoveryResult?.recovered == true
        )
    }

    @Test
    fun allRequiredPhasesShouldBePresent() {

        val result =
            runner.run(
                event =
                    createEvent(),

                duration =
                    null,

                constraint =
                    TimeConstraint(
                        type =
                            TimeConstraintType.LATEST_START,

                        start =
                            Instant.parse(
                                "2026-09-16T08:00:00Z"
                            ),
                    ),

                actualStart =
                    Instant.parse(
                        "2026-09-16T07:50:00Z"
                    ),

                actualEnd =
                    null,

                evaluatedAt =
                    evaluatedAt,
            )

        val phases =
            result.trace.steps
                .map { it.phase }
                .toSet()

        assertEquals(
            EventTimeFlowE2EPhase.entries.toSet(),
            phases,
        )
    }

    @Test
    fun persistenceShouldContainRuntimeEvent() {

        runner.run(
            event =
                createEvent(),

            duration =
                null,

            constraint =
                null,

            actualStart =
                null,

            actualEnd =
                null,

            evaluatedAt =
                evaluatedAt,
        )

        assertEquals(
            1,
            persistence.all().size,
        )
    }

    @Test
    fun recoveryShouldMatchRuntimeState() {

        val result =
            runner.run(
                event =
                    createEvent(),

                duration =
                    null,

                constraint =
                    TimeConstraint(
                        type =
                            TimeConstraintType.LATEST_START,

                        start =
                            Instant.parse(
                                "2026-09-16T08:00:00Z"
                            ),
                    ),

                actualStart =
                    Instant.parse(
                        "2026-09-16T07:55:00Z"
                    ),

                actualEnd =
                    null,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            result.trace.runtimeState,
            result.trace.recoveryResult?.state,
        )
    }

    @Test
    fun runningEventShouldProduceRunningRuntime() {

        val result =
            runner.run(
                event =
                    createEvent(),

                duration =
                    null,

                constraint =
                    null,

                actualStart =
                    null,

                actualEnd =
                    null,

                evaluatedAt =
                    evaluatedAt,
            )

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            result.trace.runtimeState,
        )
    }

    @Test
    fun repeatedRunShouldPersistMultipleRuntimeEvents() {

        runner.run(
            event =
                createEvent(),

            duration =
                null,

            constraint =
                null,

            actualStart =
                null,

            actualEnd =
                null,

            evaluatedAt =
                evaluatedAt,
        )

        runner.run(
            event =
                createEvent(),

            duration =
                null,

            constraint =
                null,

            actualStart =
                null,

            actualEnd =
                null,

            evaluatedAt =
                Instant.parse(
                    "2026-09-16T07:31:00Z"
                ),
        )

        assertEquals(
            2,
            persistence.all().size,
        )
    }
}
