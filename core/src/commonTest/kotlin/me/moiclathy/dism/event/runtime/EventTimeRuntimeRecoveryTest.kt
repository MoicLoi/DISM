package me.moiclathy.dism.event.runtime

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class EventTimeRuntimeRecoveryTest {

    private val t1 =
        Instant.parse(
            "2026-09-16T10:00:00Z"
        )

    private val t2 =
        Instant.parse(
            "2026-09-16T10:01:00Z"
        )

    private val t3 =
        Instant.parse(
            "2026-09-16T10:02:00Z"
        )

    private fun event(
        id: String,
        state: EventTimeRuntimeState,
        time: Instant,
        processed: Int,
        alarms: Int,
    ) =
        EventTimeRuntimePersistenceEvent(
            id =
                id,

            runtimeState =
                state,

            evaluatedAt =
                time,

            processedEventCount =
                processed,

            generatedAlarmCount =
                alarms,
        )

    @Test
    fun emptyPersistenceShouldNotRecover() {

        val persistence =
            InMemoryEventTimeRuntimePersistence()

        val recovery =
            DeterministicEventTimeRuntimeRecovery(
                persistence
            )

        val result =
            recovery.recover()

        assertFalse(
            result.recovered
        )

        assertEquals(
            EventTimeRuntimeState.STOPPED,
            result.state,
        )

        assertNull(
            result.restoredFromEventId
        )

        assertEquals(
            0,
            result.processedEventCount,
        )

        assertEquals(
            0,
            result.generatedAlarmCount,
        )

        assertEquals(
            0,
            result.historySize,
        )
    }

    @Test
    fun latestEventShouldDetermineRecoveredState() {

        val persistence =
            InMemoryEventTimeRuntimePersistence()

        persistence.append(
            event(
                id =
                    "runtime-1",

                state =
                    EventTimeRuntimeState.RUNNING,

                time =
                    t1,

                processed =
                    5,

                alarms =
                    0,
            )
        )

        persistence.append(
            event(
                id =
                    "runtime-2",

                state =
                    EventTimeRuntimeState.PAUSED,

                time =
                    t2,

                processed =
                    8,

                alarms =
                    1,
            )
        )

        val recovery =
            DeterministicEventTimeRuntimeRecovery(
                persistence
            )

        val result =
            recovery.recover()

        assertTrue(
            result.recovered
        )

        assertEquals(
            EventTimeRuntimeState.PAUSED,
            result.state,
        )

        assertEquals(
            "runtime-2",
            result.restoredFromEventId,
        )

        assertEquals(
            t2,
            result.restoredAt,
        )

        assertEquals(
            8,
            result.processedEventCount,
        )

        assertEquals(
            1,
            result.generatedAlarmCount,
        )

        assertEquals(
            2,
            result.historySize,
        )
    }

    @Test
    fun stoppedStateShouldBeRecoverable() {

        val persistence =
            InMemoryEventTimeRuntimePersistence()

        persistence.append(
            event(
                id =
                    "runtime-stop",

                state =
                    EventTimeRuntimeState.STOPPED,

                time =
                    t3,

                processed =
                    10,

                alarms =
                    2,
            )
        )

        val recovery =
            DeterministicEventTimeRuntimeRecovery(
                persistence
            )

        val result =
            recovery.recover()

        assertTrue(
            result.recovered
        )

        assertEquals(
            EventTimeRuntimeState.STOPPED,
            result.state,
        )

        assertEquals(
            "runtime-stop",
            result.restoredFromEventId,
        )
    }

    @Test
    fun recoveryShouldPreserveLatestCounters() {

        val persistence =
            InMemoryEventTimeRuntimePersistence()

        persistence.append(
            event(
                id =
                    "runtime-1",

                state =
                    EventTimeRuntimeState.RUNNING,

                time =
                    t1,

                processed =
                    3,

                alarms =
                    0,
            )
        )

        persistence.append(
            event(
                id =
                    "runtime-2",

                state =
                    EventTimeRuntimeState.RUNNING,

                time =
                    t2,

                processed =
                    7,

                alarms =
                    2,
            )
        )

        val result =
            DeterministicEventTimeRuntimeRecovery(
                persistence
            ).recover()

        assertEquals(
            7,
            result.processedEventCount,
        )

        assertEquals(
            2,
            result.generatedAlarmCount,
        )
    }
}
