package me.moiclathy.dism.event.runtime

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeterministicEventTimeRuntimeTest {

    private val t1 =
        Instant.parse(
            "2026-09-16T10:00:00Z"
        )

    private val t2 =
        Instant.parse(
            "2026-09-16T10:01:00Z"
        )

    @Test
    fun runtimeShouldStartFromStopped() {

        val runtime =
            DeterministicEventTimeRuntime()

        assertEquals(
            EventTimeRuntimeState.STOPPED,
            runtime.state(),
        )

        val result =
            runtime.start(
                evaluatedAt =
                    t1,
            )

        assertEquals(
            EventTimeRuntimeState.STOPPED,
            result.previousState,
        )

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            result.currentState,
        )

        assertTrue(
            result.stateChanged
        )

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            runtime.state(),
        )
    }

    @Test
    fun runningRuntimeShouldProcessTick() {

        val runtime =
            DeterministicEventTimeRuntime()

        runtime.start(t1)

        val result =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        5,
                )
            )

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            result.currentState,
        )

        assertEquals(
            5,
            result.record.processedEventCount,
        )

        assertEquals(
            0,
            result.record.generatedAlarmCount,
        )
    }

    @Test
    fun stoppedRuntimeShouldNotProcessTick() {

        val runtime =
            DeterministicEventTimeRuntime()

        val result =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t1,

                    eventCount =
                        5,
                )
            )

        assertEquals(
            EventTimeRuntimeState.STOPPED,
            result.currentState,
        )

        assertEquals(
            0,
            result.record.processedEventCount,
        )
    }

    @Test
    fun pauseShouldChangeState() {

        val runtime =
            DeterministicEventTimeRuntime()

        runtime.start(t1)

        val result =
            runtime.pause(t2)

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            result.previousState,
        )

        assertEquals(
            EventTimeRuntimeState.PAUSED,
            result.currentState,
        )

        assertTrue(
            result.stateChanged
        )

        assertEquals(
            EventTimeRuntimeState.PAUSED,
            runtime.state(),
        )
    }

    @Test
    fun pausedRuntimeShouldNotProcessTick() {

        val runtime =
            DeterministicEventTimeRuntime()

        runtime.start(t1)

        runtime.pause(t2)

        val result =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        Instant.parse(
                            "2026-09-16T10:02:00Z"
                        ),

                    eventCount =
                        10,
                )
            )

        assertEquals(
            EventTimeRuntimeState.PAUSED,
            result.currentState,
        )

        assertEquals(
            0,
            result.record.processedEventCount,
        )
    }

    @Test
    fun stopShouldChangeState() {

        val runtime =
            DeterministicEventTimeRuntime()

        runtime.start(t1)

        val result =
            runtime.stop(t2)

        assertEquals(
            EventTimeRuntimeState.RUNNING,
            result.previousState,
        )

        assertEquals(
            EventTimeRuntimeState.STOPPED,
            result.currentState,
        )

        assertTrue(
            result.stateChanged
        )
    }

    @Test
    fun repeatedTickShouldBeAllowedByDefault() {

        val runtime =
            DeterministicEventTimeRuntime()

        runtime.start(t1)

        val first =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        2,
                )
            )

        val second =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        2,
                )
            )

        assertEquals(
            2,
            first.record.processedEventCount,
        )

        assertEquals(
            2,
            second.record.processedEventCount,
        )
    }

    @Test
    fun repeatedTickCanBeDisabled() {

        val runtime =
            DeterministicEventTimeRuntime(
                policy =
                    EventTimeRuntimePolicy(
                        allowRepeatedEvaluation =
                            false,
                    )
            )

        runtime.start(t1)

        val first =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        2,
                )
            )

        val second =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        2,
                )
            )

        assertEquals(
            2,
            first.record.processedEventCount,
        )

        assertEquals(
            0,
            second.record.processedEventCount,
        )
    }

    @Test
    fun tickShouldPreserveEvaluationTime() {

        val runtime =
            DeterministicEventTimeRuntime()

        runtime.start(t1)

        val result =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        1,
                )
            )

        assertEquals(
            t2,
            result.evaluatedAt,
        )

        assertEquals(
            t2,
            result.record.evaluatedAt,
        )
    }

    @Test
    fun unchangedStateShouldReportFalse() {

        val runtime =
            DeterministicEventTimeRuntime()

        runtime.start(t1)

        val result =
            runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        1,
                )
            )

        assertFalse(
            result.stateChanged
        )
    }

    @Test
    fun runtimeShouldBeDeterministic() {

        fun run(): EventTimeRuntimeEvaluation {

            val runtime =
                DeterministicEventTimeRuntime()

            runtime.start(t1)

            return runtime.tick(
                EventTimeRuntimeInput(
                    evaluatedAt =
                        t2,

                    eventCount =
                        3,
                )
            )
        }

        assertEquals(
            run(),
            run(),
        )
    }
}
