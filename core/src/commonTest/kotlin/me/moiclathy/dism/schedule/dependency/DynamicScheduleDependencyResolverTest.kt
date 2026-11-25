package me.moiclathy.dism.schedule.dependency

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleModel
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DynamicScheduleDependencyResolverTest {
    private val scheduleId = DynamicScheduleId("schedule-1")

    private fun item(
        id: String,
        state: DynamicScheduleState,
    ) = DynamicScheduleItem(
        id = DynamicScheduleItemId(id),
        scheduleId = scheduleId,
        title = id,
        type = DynamicScheduleItemType.TASK,
        source = DynamicScheduleSource.DERIVED,
        state = state,
    )

    private fun model(
        vararg items: DynamicScheduleItem,
    ) = DynamicScheduleModel(scheduleId, items.toList())

    private fun dependency(
        id: String,
        from: String,
        to: String,
        type: DynamicScheduleDependencyType,
    ) = DynamicScheduleDependency(
        id = DynamicScheduleDependencyId(id),
        fromItemId = DynamicScheduleItemId(from),
        toItemId = DynamicScheduleItemId(to),
        type = type,
    )

    @Test
    fun requiresCompletionBlocksDependent() {
        val result = DeterministicDynamicScheduleDependencyResolver().resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("prepare", DynamicScheduleState.READY),
                    item("class", DynamicScheduleState.READY),
                ),
                dependencies = DynamicScheduleDependencySet(
                    listOf(
                        dependency(
                            "d1", "prepare", "class",
                            DynamicScheduleDependencyType.REQUIRES_COMPLETION,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            DynamicScheduleState.BLOCKED,
            result.projectedModel.item(DynamicScheduleItemId("class"))?.state,
        )
        assertEquals(1, result.changes.size)
        assertFalse(result.matches.single().satisfied)
    }

    @Test
    fun requiresCompletionIsSatisfiedAfterSourceCompletes() {
        val result = DeterministicDynamicScheduleDependencyResolver().resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("prepare", DynamicScheduleState.COMPLETED),
                    item("class", DynamicScheduleState.READY),
                ),
                dependencies = DynamicScheduleDependencySet(
                    listOf(
                        dependency(
                            "d1", "prepare", "class",
                            DynamicScheduleDependencyType.REQUIRES_COMPLETION,
                        ),
                    ),
                ),
            ),
        )

        assertTrue(result.matches.single().satisfied)
        assertFalse(result.changed)
    }

    @Test
    fun requiresStartAcceptsActiveAndCompletedSource() {
        val resolver = DeterministicDynamicScheduleDependencyResolver()
        val dep = DynamicScheduleDependencySet(
            listOf(
                dependency(
                    "d1", "prepare", "class",
                    DynamicScheduleDependencyType.REQUIRES_START,
                ),
            ),
        )

        val active = resolver.resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("prepare", DynamicScheduleState.ACTIVE),
                    item("class", DynamicScheduleState.READY),
                ),
                dependencies = dep,
            ),
        )
        val completed = resolver.resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("prepare", DynamicScheduleState.COMPLETED),
                    item("class", DynamicScheduleState.READY),
                ),
                dependencies = dep,
            ),
        )

        assertTrue(active.matches.single().satisfied)
        assertTrue(completed.matches.single().satisfied)
    }

    @Test
    fun associatedIsNonBlockingByDefault() {
        val result = DeterministicDynamicScheduleDependencyResolver().resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("a", DynamicScheduleState.READY),
                    item("b", DynamicScheduleState.READY),
                ),
                dependencies = DynamicScheduleDependencySet(
                    listOf(
                        dependency(
                            "d1", "a", "b",
                            DynamicScheduleDependencyType.ASSOCIATED,
                        ),
                    ),
                ),
            ),
        )

        assertFalse(result.changed)
        assertTrue(result.matches.single().satisfied)
    }

    @Test
    fun topologicalOrderIsDeterministic() {
        val result = DeterministicDynamicScheduleDependencyResolver().resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("c", DynamicScheduleState.READY),
                    item("a", DynamicScheduleState.COMPLETED),
                    item("b", DynamicScheduleState.COMPLETED),
                ),
                dependencies = DynamicScheduleDependencySet(
                    listOf(
                        dependency("d1", "a", "c", DynamicScheduleDependencyType.PRECEDES),
                        dependency("d2", "b", "c", DynamicScheduleDependencyType.PRECEDES),
                    ),
                ),
            ),
        )

        assertEquals(listOf("a", "b", "c"), result.topologicalOrder.map { it.value })
    }

    @Test
    fun cycleIsDetected() {
        val result = DeterministicDynamicScheduleDependencyResolver().resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("a", DynamicScheduleState.READY),
                    item("b", DynamicScheduleState.READY),
                ),
                dependencies = DynamicScheduleDependencySet(
                    listOf(
                        dependency("d1", "a", "b", DynamicScheduleDependencyType.PRECEDES),
                        dependency("d2", "b", "a", DynamicScheduleDependencyType.PRECEDES),
                    ),
                ),
            ),
        )

        assertTrue(result.hasCycles)
        assertTrue(result.cycles.isNotEmpty())
    }

    @Test
    fun terminalDependentIsPreserved() {
        val result = DeterministicDynamicScheduleDependencyResolver().resolve(
            DynamicScheduleDependencyInput(
                model = model(
                    item("prepare", DynamicScheduleState.READY),
                    item("class", DynamicScheduleState.COMPLETED),
                ),
                dependencies = DynamicScheduleDependencySet(
                    listOf(
                        dependency(
                            "d1", "prepare", "class",
                            DynamicScheduleDependencyType.REQUIRES_COMPLETION,
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            DynamicScheduleState.COMPLETED,
            result.projectedModel.item(DynamicScheduleItemId("class"))?.state,
        )
    }
}
