package me.moiclathy.dism.event.model

import kotlin.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EventTest {

    private val start =
        Instant.parse(
            "2026-09-16T08:00:00Z",
        )

    @Test
    fun eventShouldPreserveIdentity() {

        val event =
            Event(
                id =
                    EventId(
                        "event:test:001",
                    ),
                title =
                    "实验课",
                type =
                    EventType.CLASS,
            )

        assertEquals(
            "event:test:001",
            event.id.value,
        )
    }

    @Test
    fun eventShouldDefaultToUnscheduled() {

        val event =
            Event(
                id =
                    EventId(
                        "event:test:002",
                    ),
                title =
                    "买实验服",
                type =
                    EventType.TASK,
            )

        assertEquals(
            EventTemporal.Unscheduled,
            event.temporal,
        )
    }

    @Test
    fun eventShouldPreserveTemporalInformation() {

        val event =
            Event(
                id =
                    EventId(
                        "event:test:003",
                    ),
                title =
                    "实验课",
                type =
                    EventType.CLASS,
                temporal =
                    EventTemporal.At(
                        start,
                    ),
            )

        assertEquals(
            EventTemporal.At(start),
            event.temporal,
        )
    }

    @Test
    fun eventShouldPreserveSource() {

        val event =
            Event(
                id =
                    EventId(
                        "event:test:004",
                    ),
                title =
                    "数据库同步课程",
                type =
                    EventType.CLASS,
                source =
                    EventSource.EXTERNAL_SCHEDULE,
            )

        assertEquals(
            EventSource.EXTERNAL_SCHEDULE,
            event.source,
        )
    }

    @Test
    fun eventShouldPreserveExternalId() {

        val event =
            Event(
                id =
                    EventId(
                        "event:test:005",
                    ),
                title =
                    "实验课",
                type =
                    EventType.CLASS,
                externalId =
                    "xiqueer:course:123",
            )

        assertEquals(
            "xiqueer:course:123",
            event.externalId,
        )
    }

    @Test
    fun metadataShouldBeAccessible() {

        val event =
            Event(
                id =
                    EventId(
                        "event:test:006",
                    ),
                title =
                    "实验课",
                type =
                    EventType.CLASS,
                metadata =
                    EventMetadata(
                        mapOf(
                            "courseCode" to "LAB-101",
                            "room" to "实验楼101",
                        ),
                    ),
            )

        assertEquals(
            "LAB-101",
            event.metadata["courseCode"],
        )

        assertEquals(
            "实验楼101",
            event.metadata["room"],
        )

        assertTrue(
            event.metadata.contains(
                "room",
            ),
        )
    }

    @Test
    fun factoryShouldCreateEquivalentEvent() {

        val event =
            EventFactory.create(
                id =
                    EventId(
                        "event:test:007",
                    ),
                title =
                    "考试",
                type =
                    EventType.EXAM,
                temporal =
                    EventTemporal.At(
                        start,
                    ),
                source =
                    EventSource.USER_INPUT,
            )

        val expected =
            Event(
                id =
                    EventId(
                        "event:test:007",
                    ),
                title =
                    "考试",
                type =
                    EventType.EXAM,
                temporal =
                    EventTemporal.At(
                        start,
                    ),
                source =
                    EventSource.USER_INPUT,
            )

        assertEquals(
            expected,
            event,
        )
    }

    @Test
    fun sameEventShouldRemainEqualWhenReconstructed() {

        val first =
            Event(
                id =
                    EventId(
                        "event:test:008",
                    ),
                title =
                    "实验课",
                type =
                    EventType.CLASS,
                temporal =
                    EventTemporal.At(
                        start,
                    ),
            )

        val second =
            first.copy()

        assertEquals(
            first,
            second,
        )
    }
}
