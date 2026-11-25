package me.moiclathy.dism.context.e2e

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import me.moiclathy.dism.context.model.ContextMode
import me.moiclathy.dism.context.runtime.InMemoryContextRuntimeEventLog

class ContextRuntimeE2ERunnerTest {

    private val t1 =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    private val t2 =
        Instant.parse(
            "2026-09-16T09:00:00Z",
        )

    @Test
    fun fullContextRuntimeE2E() {

        val eventLog =
            InMemoryContextRuntimeEventLog()

        val runner =
            ContextRuntimeE2ERunner(
                eventLog = eventLog,
            )

        val result =
            runner.run(
                initialMode =
                    ContextMode.IN_CLASS,

                initialConfidence =
                    0.90,

                nextMode =
                    ContextMode.IN_TRANSIT,

                nextConfidence =
                    0.85,

                firstObservedAt =
                    t1,

                transitionedAt =
                    t2,
            )

        assertTrue(
            result.success,
            result.failures.joinToString(),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.SOURCE,
            ),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.RESOLUTION,
            ),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.TRANSITION,
            ),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.CONFIDENCE,
            ),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.RUNTIME_EVENT,
            ),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.PERSISTED,
            ),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.RECOVERY,
            ),
        )

        assertTrue(
            result.trace.contains(
                ContextRuntimeE2EPhase.FINAL_STATE,
            ),
        )
    }

    @Test
    fun recoveredStateMatchesFinalContext() {

        val eventLog =
            InMemoryContextRuntimeEventLog()

        val runner =
            ContextRuntimeE2ERunner(
                eventLog = eventLog,
            )

        val result =
            runner.run(
                initialMode =
                    ContextMode.IN_CLASS,

                initialConfidence =
                    0.90,

                nextMode =
                    ContextMode.IN_TRANSIT,

                nextConfidence =
                    0.85,

                firstObservedAt =
                    t1,

                transitionedAt =
                    t2,
            )

        val state =
            result.trace.recoveredState

        assertEquals(
            ContextMode.IN_TRANSIT,
            state?.activeMode,
        )

        assertEquals(
            0.85,
            state?.confidence?.value ?: 0.0,
            0.000001,
        )
    }

    @Test
    fun eventsAreActuallyPersisted() {

        val eventLog =
            InMemoryContextRuntimeEventLog()

        val runner =
            ContextRuntimeE2ERunner(
                eventLog = eventLog,
            )

        runner.run(
            initialMode =
                ContextMode.IN_CLASS,

            initialConfidence =
                0.90,

            nextMode =
                ContextMode.IN_TRANSIT,

            nextConfidence =
                0.85,

            firstObservedAt =
                t1,

            transitionedAt =
                t2,
        )

        assertEquals(
            3,
            eventLog.all().size,
        )
    }

    @Test
    fun recoveryCanBeRepeatedDeterministically() {

        val eventLog =
            InMemoryContextRuntimeEventLog()

        val runner =
            ContextRuntimeE2ERunner(
                eventLog = eventLog,
            )

        runner.run(
            initialMode =
                ContextMode.IN_CLASS,

            initialConfidence =
                0.90,

            nextMode =
                ContextMode.IN_TRANSIT,

            nextConfidence =
                0.85,

            firstObservedAt =
                t1,

            transitionedAt =
                t2,
        )

        val first =
            runner.recoverOnly()

        val second =
            runner.recoverOnly()

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun duplicatePersistenceDoesNotChangeRecoveredState() {

        val eventLog =
            InMemoryContextRuntimeEventLog()

        val runner =
            ContextRuntimeE2ERunner(
                eventLog = eventLog,
            )

        runner.run(
            initialMode =
                ContextMode.IN_CLASS,

            initialConfidence =
                0.90,

            nextMode =
                ContextMode.IN_TRANSIT,

            nextConfidence =
                0.85,

            firstObservedAt =
                t1,

            transitionedAt =
                t2,
        )

        val before =
            runner.recoverOnly()

        val duplicatedEvents =
            eventLog.all()

        eventLog.appendAll(
            duplicatedEvents,
        )

        val after =
            runner.recoverOnly()

        assertEquals(
            before,
            after,
        )
    }
}
