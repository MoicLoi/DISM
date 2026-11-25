package me.moiclathy.dism.event.state

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeterministicEventStateTransitionEngineTest {

    private val engine =
        DeterministicEventStateTransitionEngine()

    private val now =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    @Test
    fun firstStateShouldBeInitialized() {

        val result =
            engine.transition(
                eventId =
                    "event:experiment-class",

                previousState =
                    null,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.INITIALIZED,
            result.transition?.type,
        )

        assertNull(
            result.transition?.from,
        )

        assertEquals(
            EventState.PLANNED,
            result.transition?.to,
        )

        assertEquals(
            EventState.PLANNED,
            result.currentState,
        )

        assertTrue(
            result.changed,
        )
    }

    @Test
    fun plannedToActiveShouldChange() {

        val result =
            engine.transition(
                eventId =
                    "event:experiment-class",

                previousState =
                    EventState.PLANNED,

                currentState =
                    EventState.ACTIVE,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.CHANGED,
            result.transition?.type,
        )

        assertEquals(
            EventState.PLANNED,
            result.transition?.from,
        )

        assertEquals(
            EventState.ACTIVE,
            result.transition?.to,
        )

        assertTrue(
            result.changed,
        )
    }

    @Test
    fun activeToCompletedShouldChange() {

        val result =
            engine.transition(
                eventId =
                    "event:experiment-class",

                previousState =
                    EventState.ACTIVE,

                currentState =
                    EventState.COMPLETED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.CHANGED,
            result.transition?.type,
        )

        assertEquals(
            EventState.ACTIVE,
            result.transition?.from,
        )

        assertEquals(
            EventState.COMPLETED,
            result.transition?.to,
        )
    }

    @Test
    fun sameStateShouldNotEmitByDefault() {

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.PLANNED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertNull(
            result.transition,
        )

        assertEquals(
            EventState.PLANNED,
            result.currentState,
        )

        assertTrue(
            !result.changed,
        )
    }

    @Test
    fun sameStateCanEmitPersisting() {

        val engine =
            DeterministicEventStateTransitionEngine(
                EventStateTransitionPolicy(
                    emitPersisting = true,
                ),
            )

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.PLANNED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.PERSISTING,
            result.transition?.type,
        )

        assertEquals(
            EventState.PLANNED,
            result.transition?.from,
        )

        assertEquals(
            EventState.PLANNED,
            result.transition?.to,
        )
    }

    @Test
    fun postponedShouldBeAllowed() {

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.PLANNED,

                currentState =
                    EventState.POSTPONED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.CHANGED,
            result.transition?.type,
        )
    }

    @Test
    fun postponedCanReturnToPlanned() {

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.POSTPONED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.CHANGED,
            result.transition?.type,
        )
    }

    @Test
    fun expiredCanReturnToPlannedByDefault() {

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.EXPIRED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.CHANGED,
            result.transition?.type,
        )
    }

    @Test
    fun expiredCannotReturnWhenPolicyDisallows() {

        val engine =
            DeterministicEventStateTransitionEngine(
                EventStateTransitionPolicy(
                    allowExpiredReopen = false,
                ),
            )

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.EXPIRED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertNull(
            result.transition,
        )

        assertEquals(
            EventState.EXPIRED,
            result.currentState,
        )
    }

    @Test
    fun completedShouldNotReopenByDefault() {

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.COMPLETED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertNull(
            result.transition,
        )

        assertEquals(
            EventState.COMPLETED,
            result.currentState,
        )
    }

    @Test
    fun completedCanReopenWhenConfigured() {

        val engine =
            DeterministicEventStateTransitionEngine(
                EventStateTransitionPolicy(
                    allowCompletedReopen = true,
                ),
            )

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.COMPLETED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.CHANGED,
            result.transition?.type,
        )
    }

    @Test
    fun cancelledShouldNotReopenByDefault() {

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.CANCELLED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertNull(
            result.transition,
        )

        assertEquals(
            EventState.CANCELLED,
            result.currentState,
        )
    }

    @Test
    fun cancelledCanReopenWhenConfigured() {

        val engine =
            DeterministicEventStateTransitionEngine(
                EventStateTransitionPolicy(
                    allowCancelledReopen = true,
                ),
            )

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.CANCELLED,

                currentState =
                    EventState.PLANNED,

                evaluatedAt =
                    now,
            )

        assertEquals(
            EventTransitionType.CHANGED,
            result.transition?.type,
        )
    }

    @Test
    fun reasonShouldBePreserved() {

        val result =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.PLANNED,

                currentState =
                    EventState.ACTIVE,

                evaluatedAt =
                    now,

                reason =
                    "event started",
            )

        assertEquals(
            "event started",
            result.transition?.reason,
        )
    }

    @Test
    fun transitionShouldBeDeterministic() {

        val first =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.PLANNED,

                currentState =
                    EventState.ACTIVE,

                evaluatedAt =
                    now,

                reason =
                    "started",
            )

        val second =
            engine.transition(
                eventId =
                    "event:test",

                previousState =
                    EventState.PLANNED,

                currentState =
                    EventState.ACTIVE,

                evaluatedAt =
                    now,

                reason =
                    "started",
            )

        assertEquals(
            first,
            second,
        )
    }
}
