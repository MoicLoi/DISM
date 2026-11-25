package me.moiclathy.dism.event.dependency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DeterministicEventDependencyEngineTest {

    private val engine =
        DeterministicEventDependencyEngine()

    @Test
    fun simpleDependencyShouldBeValid() {

        val dependency =
            EventDependency(
                "event:class",
                "event:arrive",
                EventDependencyType.REQUIRES_COMPLETION,
            )

        val result =
            engine.addDependency(
                EventDependencyGraph(),
                dependency,
            )

        assertTrue(result.valid)

        assertEquals(
            listOf(dependency),
            result.graph.dependencies,
        )
    }

    @Test
    fun duplicateDependencyShouldBeRejected() {

        val dependency =
            EventDependency(
                "event:class",
                "event:arrive",
                EventDependencyType.REQUIRES_COMPLETION,
            )

        val graph =
            EventDependencyGraph(
                listOf(dependency)
            )

        val result =
            engine.addDependency(
                graph,
                dependency,
            )

        assertFalse(result.valid)

        assertTrue(
            result.errors.any {
                it.contains("Duplicate")
            }
        )
    }

    @Test
    fun simpleChainShouldBeValid() {

        val graph =
            EventDependencyGraph(
                listOf(
                    EventDependency(
                        "event:a",
                        "event:b",
                        EventDependencyType.PRECEDES,
                    ),
                    EventDependency(
                        "event:b",
                        "event:c",
                        EventDependencyType.PRECEDES,
                    ),
                ),
            )

        val result =
            engine.validate(graph)

        assertTrue(result.valid)
    }

    @Test
    fun cycleShouldBeRejected() {

        val graph =
            EventDependencyGraph(
                listOf(
                    EventDependency(
                        "event:a",
                        "event:b",
                        EventDependencyType.PRECEDES,
                    ),
                    EventDependency(
                        "event:b",
                        "event:c",
                        EventDependencyType.PRECEDES,
                    ),
                    EventDependency(
                        "event:c",
                        "event:a",
                        EventDependencyType.PRECEDES,
                    ),
                ),
            )

        val result =
            engine.validate(graph)

        assertFalse(result.valid)

        assertTrue(
            result.errors.any {
                it.contains("cycle", ignoreCase = true)
            }
        )
    }

    @Test
    fun associatedDependencyShouldNotCreateCycleByDefault() {

        val graph =
            EventDependencyGraph(
                listOf(
                    EventDependency(
                        "event:a",
                        "event:b",
                        EventDependencyType.ASSOCIATED,
                    ),
                    EventDependency(
                        "event:b",
                        "event:a",
                        EventDependencyType.ASSOCIATED,
                    ),
                ),
            )

        val result =
            engine.validate(graph)

        assertTrue(result.valid)
    }

    @Test
    fun associatedDependencyCanParticipateInOrdering() {

        val engine =
            DeterministicEventDependencyEngine(
                EventDependencyPolicy(
                    associatedParticipatesInOrdering = true,
                ),
            )

        val graph =
            EventDependencyGraph(
                listOf(
                    EventDependency(
                        "event:a",
                        "event:b",
                        EventDependencyType.ASSOCIATED,
                    ),
                    EventDependency(
                        "event:b",
                        "event:a",
                        EventDependencyType.ASSOCIATED,
                    ),
                ),
            )

        val result =
            engine.validate(graph)

        assertFalse(result.valid)
    }

    @Test
    fun cycleCanBeAllowedByPolicy() {

        val engine =
            DeterministicEventDependencyEngine(
                EventDependencyPolicy(
                    allowCycles = true,
                ),
            )

        val graph =
            EventDependencyGraph(
                listOf(
                    EventDependency(
                        "event:a",
                        "event:b",
                        EventDependencyType.PRECEDES,
                    ),
                    EventDependency(
                        "event:b",
                        "event:a",
                        EventDependencyType.PRECEDES,
                    ),
                ),
            )

        val result =
            engine.validate(graph)

        assertTrue(result.valid)
    }

    @Test
    fun removeShouldReturnNewGraph() {

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

        val updated =
            engine.removeDependency(
                graph,
                dependency,
            )

        assertTrue(
            updated.dependencies.isEmpty()
        )

        assertEquals(
            listOf(dependency),
            graph.dependencies,
        )
    }

    @Test
    fun engineShouldBeDeterministic() {

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

        val first =
            engine.validate(graph)

        val second =
            engine.validate(graph)

        assertEquals(
            first,
            second,
        )
    }
}
