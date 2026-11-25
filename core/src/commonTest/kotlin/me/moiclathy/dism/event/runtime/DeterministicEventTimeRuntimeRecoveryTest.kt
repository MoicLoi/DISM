package me.moiclathy.dism.event.runtime

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class DeterministicEventTimeRuntimeRecoveryTest {

    private val persistence =
        InMemoryEventTimeRuntimePersistence()

    private val evaluatedAt =
        Instant.parse(
            "2026-09-16T12:00:00Z"
        )

    @Test
    fun recoveryShouldBeDeterministic() {

        persistence.append(
            EventTimeRuntimePersistenceEvent(
                id =
                    "runtime-1",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    20,

                generatedAlarmCount =
                    3,

                metadata =
                    mapOf(
                        "source" to "test",
                    ),
            )
        )

        val recovery =
            DeterministicEventTimeRuntimeRecovery(
                persistence
            )

        val first =
            recovery.recover()

        val second =
            recovery.recover()

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun recoveryShouldNotMutatePersistence() {

        persistence.clear()

        persistence.append(
            EventTimeRuntimePersistenceEvent(
                id =
                    "runtime-1",

                runtimeState =
                    EventTimeRuntimeState.RUNNING,

                evaluatedAt =
                    evaluatedAt,

                processedEventCount =
                    20,

                generatedAlarmCount =
                    3,
            )
        )

        val before =
            persistence.all()

        DeterministicEventTimeRuntimeRecovery(
            persistence
        ).recover()

        val after =
            persistence.all()

        assertEquals(
            before,
            after,
        )
    }
}
