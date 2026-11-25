package me.moiclathy.dism.schedule.context

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

class DeterministicDynamicScheduleContextBinderTest {

    private val scheduleId =
        DynamicScheduleId("schedule-1")

    private fun model(
        state: DynamicScheduleState =
            DynamicScheduleState.READY,
    ): DynamicScheduleModel {

        val item = DynamicScheduleItem(
            id = DynamicScheduleItemId("item-1"),
            scheduleId = scheduleId,
            title = "Bring charger",
            type = DynamicScheduleItemType.TASK,
            source = DynamicScheduleSource.DERIVED,
            state = state,
        )

        return DynamicScheduleModel(
            id = scheduleId,
            items = listOf(item),
        )
    }

    private fun binding(
        id: String,
        mode: DynamicScheduleContextBindingMode,
        key: String,
    ): DynamicScheduleContextBinding {

        return DynamicScheduleContextBinding(
            id = DynamicScheduleContextBindingId(id),
            itemId = DynamicScheduleItemId("item-1"),
            contextKey = key,
            mode = mode,
        )
    }

    @Test
    fun missingRequiredContextBlocksItem() {

        val result =
            DeterministicDynamicScheduleContextBinder().bind(
                DynamicScheduleContextBindingInput(
                    model = model(),

                    bindings =
                        DynamicScheduleContextBindingSet(
                            listOf(
                                binding(
                                    id = "b1",
                                    mode =
                                        DynamicScheduleContextBindingMode.REQUIRED,
                                    key = "DORM",
                                ),
                            ),
                        ),

                    activeContextKeys = emptySet(),
                ),
            )

        assertEquals(
            DynamicScheduleState.BLOCKED,
            result.projectedModel
                .item(DynamicScheduleItemId("item-1"))
                ?.state,
        )

        assertEquals(
            DynamicScheduleContextBindingChangeReason
                .REQUIRED_CONTEXT_MISSING,
            result.changes.single().reason,
        )
    }

    @Test
    fun satisfiedRequiredContextDoesNotBlock() {

        val result =
            DeterministicDynamicScheduleContextBinder().bind(
                DynamicScheduleContextBindingInput(
                    model = model(),

                    bindings =
                        DynamicScheduleContextBindingSet(
                            listOf(
                                binding(
                                    id = "b1",
                                    mode =
                                        DynamicScheduleContextBindingMode.REQUIRED,
                                    key = "DORM",
                                ),
                            ),
                        ),

                    activeContextKeys = setOf("DORM"),
                ),
            )

        assertFalse(result.changed)

        assertTrue(
            result.matches.single().contextSatisfied,
        )
    }

    @Test
    fun excludedContextBlocksItem() {

        val result =
            DeterministicDynamicScheduleContextBinder().bind(
                DynamicScheduleContextBindingInput(
                    model = model(),

                    bindings =
                        DynamicScheduleContextBindingSet(
                            listOf(
                                binding(
                                    id = "b1",
                                    mode =
                                        DynamicScheduleContextBindingMode.EXCLUDED,
                                    key = "CLASS",
                                ),
                            ),
                        ),

                    activeContextKeys = setOf("CLASS"),
                ),
            )

        assertEquals(
            DynamicScheduleState.BLOCKED,
            result.projectedModel
                .item(DynamicScheduleItemId("item-1"))
                ?.state,
        )

        assertTrue(
            result.matches.single().excludedSatisfied,
        )
    }

    @Test
    fun blockedItemCanReturnToReadyWhenContextIsSatisfied() {

        val result =
            DeterministicDynamicScheduleContextBinder().bind(
                DynamicScheduleContextBindingInput(
                    model =
                        model(
                            DynamicScheduleState.BLOCKED,
                        ),

                    bindings =
                        DynamicScheduleContextBindingSet(
                            listOf(
                                binding(
                                    id = "b1",
                                    mode =
                                        DynamicScheduleContextBindingMode.REQUIRED,
                                    key = "DORM",
                                ),
                            ),
                        ),

                    activeContextKeys = setOf("DORM"),

                    policy =
                        DynamicScheduleContextBindingPolicy(
                            preserveExplicitBlockedState = false,
                        ),
                ),
            )

        assertEquals(
            DynamicScheduleState.READY,
            result.projectedModel
                .item(DynamicScheduleItemId("item-1"))
                ?.state,
        )

        assertEquals(
            DynamicScheduleContextBindingChangeReason
                .CONTEXT_SATISFIED,
            result.changes.single().reason,
        )
    }

    @Test
    fun preferredContextProducesDeterministicScore() {

        val result =
            DeterministicDynamicScheduleContextBinder().bind(
                DynamicScheduleContextBindingInput(
                    model = model(),

                    bindings =
                        DynamicScheduleContextBindingSet(
                            listOf(
                                binding(
                                    id = "b1",
                                    mode =
                                        DynamicScheduleContextBindingMode.PREFERRED,
                                    key = "DORM",
                                ).copy(
                                    weight = 1.0,
                                ),

                                binding(
                                    id = "b2",
                                    mode =
                                        DynamicScheduleContextBindingMode.PREFERRED,
                                    key = "LIBRARY",
                                ).copy(
                                    weight = 0.5,
                                ),
                            ),
                        ),

                    activeContextKeys = setOf("DORM"),
                ),
            )

        assertEquals(
            2.0 / 3.0,
            result.matches.single().preferredScore,
        )
    }
}
