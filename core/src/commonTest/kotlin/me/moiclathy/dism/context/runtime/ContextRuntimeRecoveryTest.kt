package me.moiclathy.dism.context.runtime

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import me.moiclathy.dism.context.model.ContextMode

class ContextRuntimeRecoveryTest {

    private val recovery =
        ContextRuntimeRecovery()

    private val t1 =
        Instant.parse(
            "2026-09-16T08:00:00Z"
        )

    private val t2 =
        Instant.parse(
            "2026-09-16T09:00:00Z"
        )

    private val t3 =
        Instant.parse(
            "2026-09-16T10:00:00Z"
        )

    @Test
    fun observedContextCanBeRecovered() {

        val events =
            listOf(
                ContextRuntimeEvent(
                    id = "event-1",
                    type =
                        ContextRuntimeEventType.OBSERVED,
                    occurredAt = t1,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.80,
                ),
            )

        val result =
            recovery.recover(events)

        val record =
            result.records[
                ContextMode.IN_CLASS
            ]

        assertTrue(record != null)

        assertEquals(
            1,
            record.observationCount,
        )

        assertEquals(
            0.80,
            record.lastConfidence,
            0.000001,
        )
    }

    @Test
    fun resolvedContextBecomesActive() {

        val events =
            listOf(
                ContextRuntimeEvent(
                    id = "event-1",
                    type =
                        ContextRuntimeEventType.RESOLVED,
                    occurredAt = t1,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.90,
                ),
            )

        val result =
            recovery.recover(events)

        assertEquals(
            ContextMode.IN_CLASS,
            result.activeMode,
        )

        val confidence =
            result.confidence

        assertTrue(confidence != null)

        assertEquals(
            0.90,
            confidence.value,
            0.000001,
        )
    }

    @Test
    fun transitionChangesActiveContext() {

        val events =
            listOf(
                ContextRuntimeEvent(
                    id = "event-1",
                    type =
                        ContextRuntimeEventType.RESOLVED,
                    occurredAt = t1,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.90,
                ),

                ContextRuntimeEvent(
                    id = "event-2",
                    type =
                        ContextRuntimeEventType.TRANSITION,
                    occurredAt = t2,
                    previousMode =
                        ContextMode.IN_CLASS,
                    mode =
                        ContextMode.IN_TRANSIT,
                    confidence = 0.85,
                ),
            )

        val result =
            recovery.recover(events)

        assertEquals(
            ContextMode.IN_TRANSIT,
            result.activeMode,
        )

        val transitionConfidence =
            result.confidence

        assertTrue(transitionConfidence != null)

        assertEquals(
            0.85,
            transitionConfidence.value,
            0.000001,
        )

        assertEquals(
            t2,
            result.records[
                ContextMode.IN_CLASS
            ]?.exitedAt,
        )

        assertEquals(
            t2,
            result.records[
                ContextMode.IN_TRANSIT
            ]?.enteredAt,
        )
    }

    @Test
    fun confidenceChangeUpdatesActiveConfidence() {

        val events =
            listOf(
                ContextRuntimeEvent(
                    id = "event-1",
                    type =
                        ContextRuntimeEventType.RESOLVED,
                    occurredAt = t1,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.90,
                ),

                ContextRuntimeEvent(
                    id = "event-2",
                    type =
                        ContextRuntimeEventType
                            .CONFIDENCE_CHANGED,
                    occurredAt = t2,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.45,
                ),
            )

        val result =
            recovery.recover(events)

        val changedConfidence =
            result.confidence

        assertTrue(changedConfidence != null)

        assertEquals(
            0.45,
            changedConfidence.value,
            0.000001,
        )
    }

    @Test
    fun expirationClearsActiveContext() {

        val events =
            listOf(
                ContextRuntimeEvent(
                    id = "event-1",
                    type =
                        ContextRuntimeEventType.RESOLVED,
                    occurredAt = t1,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.90,
                ),

                ContextRuntimeEvent(
                    id = "event-2",
                    type =
                        ContextRuntimeEventType.EXPIRED,
                    occurredAt = t2,
                    mode =
                        ContextMode.IN_CLASS,
                ),
            )

        val result =
            recovery.recover(events)

        assertNull(
            result.activeMode,
        )

        assertNull(
            result.confidence,
        )
    }

    @Test
    fun duplicateEventsAreIgnored() {

        val event =
            ContextRuntimeEvent(
                id = "event-1",
                type =
                    ContextRuntimeEventType.RESOLVED,
                occurredAt = t1,
                mode =
                    ContextMode.IN_CLASS,
                confidence = 0.90,
            )

        val result =
            recovery.recover(
                listOf(
                    event,
                    event,
                )
            )

        assertEquals(
            1,
            result.eventCount,
        )
    }

    @Test
    fun eventsAreRecoveredDeterministically() {

        val events =
            listOf(
                ContextRuntimeEvent(
                    id = "b",
                    type =
                        ContextRuntimeEventType.RESOLVED,
                    occurredAt = t2,
                    mode =
                        ContextMode.IN_TRANSIT,
                    confidence = 0.70,
                ),

                ContextRuntimeEvent(
                    id = "a",
                    type =
                        ContextRuntimeEventType.RESOLVED,
                    occurredAt = t1,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.90,
                ),

                ContextRuntimeEvent(
                    id = "c",
                    type =
                        ContextRuntimeEventType
                            .CONFIDENCE_CHANGED,
                    occurredAt = t3,
                    mode =
                        ContextMode.IN_TRANSIT,
                    confidence = 0.60,
                ),
            )

        val first =
            recovery.recover(events)

        val second =
            recovery.recover(events.reversed())

        assertEquals(
            first,
            second,
        )
    }

    @Test
    fun eventCountReflectsDistinctEvents() {

        val events =
            listOf(
                ContextRuntimeEvent(
                    id = "a",
                    type =
                        ContextRuntimeEventType.OBSERVED,
                    occurredAt = t1,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.80,
                ),

                ContextRuntimeEvent(
                    id = "b",
                    type =
                        ContextRuntimeEventType.RESOLVED,
                    occurredAt = t2,
                    mode =
                        ContextMode.IN_CLASS,
                    confidence = 0.90,
                ),
            )

        val result =
            recovery.recover(events)

        assertEquals(
            2,
            result.eventCount,
        )
    }
}
