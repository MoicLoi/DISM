package me.moiclathy.dism.event.dependency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class EventDependencyTest {

    @Test
    fun dependencyShouldStoreBothEvents() {

        val dependency =
            EventDependency(
                subjectEventId =
                    "event:class",

                objectEventId =
                    "event:arrive",

                type =
                    EventDependencyType.REQUIRES_COMPLETION,
            )

        assertEquals(
            "event:class",
            dependency.subjectEventId,
        )

        assertEquals(
            "event:arrive",
            dependency.objectEventId,
        )

        assertEquals(
            EventDependencyType.REQUIRES_COMPLETION,
            dependency.type,
        )
    }

    @Test
    fun selfDependencyShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            EventDependency(
                subjectEventId =
                    "event:test",

                objectEventId =
                    "event:test",

                type =
                    EventDependencyType.PRECEDES,
            )
        }
    }

    @Test
    fun blankEventIdShouldFail() {

        assertFailsWith<IllegalArgumentException> {

            EventDependency(
                subjectEventId =
                    "",

                objectEventId =
                    "event:test",

                type =
                    EventDependencyType.PRECEDES,
            )
        }
    }

    @Test
    fun graphShouldExposeEventIds() {

        val graph =
            EventDependencyGraph(
                dependencies =
                    listOf(
                        EventDependency(
                            "event:a",
                            "event:b",
                            EventDependencyType.PRECEDES,
                        ),
                        EventDependency(
                            "event:c",
                            "event:b",
                            EventDependencyType.PRECEDES,
                        ),
                    ),
            )

        assertEquals(
            setOf(
                "event:a",
                "event:b",
                "event:c",
            ),
            graph.eventIds,
        )
    }

    @Test
    fun graphShouldFindDependenciesOfEvent() {

        val dependency =
            EventDependency(
                "event:a",
                "event:b",
                EventDependencyType.PRECEDES,
            )

        val graph =
            EventDependencyGraph(
                listOf(dependency)
            )

        assertEquals(
            listOf(dependency),
            graph.dependenciesOf("event:a"),
        )
    }

    @Test
    fun graphShouldFindDependentsOfEvent() {

        val dependency =
            EventDependency(
                "event:a",
                "event:b",
                EventDependencyType.PRECEDES,
            )

        val graph =
            EventDependencyGraph(
                listOf(dependency)
            )

        assertEquals(
            listOf(dependency),
            graph.dependentsOf("event:b"),
        )
    }

    @Test
    fun graphAddShouldNotMutateOriginal() {

        val original =
            EventDependencyGraph()

        val dependency =
            EventDependency(
                "event:a",
                "event:b",
                EventDependencyType.PRECEDES,
            )

        val updated =
            original.add(dependency)

        assertTrue(
            original.dependencies.isEmpty()
        )

        assertEquals(
            listOf(dependency),
            updated.dependencies,
        )
    }
}
