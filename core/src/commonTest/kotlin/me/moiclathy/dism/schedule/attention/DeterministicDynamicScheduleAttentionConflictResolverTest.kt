package me.moiclathy.dism.schedule.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItemId

class DeterministicDynamicScheduleAttentionConflictResolverTest {

    private val resolver =
        DeterministicDynamicScheduleAttentionConflictResolver()

    private val itemId =
        DynamicScheduleItemId("item-1")

    @Test
    fun noHardConstraintAllowsMemoryToSuppress() {

        val result =
            resolver.resolve(
                DynamicScheduleAttentionConflictInput(
                    signal =
                        DynamicScheduleAttentionSignal(
                            itemId = itemId,
                            schedulePriority = 0.8,
                            globalMemoryFactor = 0.5,
                            contextMemoryFactor = 1.0,
                        ),
                ),
            )

        assertEquals(
            0.4,
            result.resolvedPriority,
            0.0001,
        )

        assertTrue(
            !result.hardConstraintPresent,
        )
    }

    @Test
    fun requiredDependencyOverridesLearnedSuppression() {

        val result =
            resolver.resolve(
                DynamicScheduleAttentionConflictInput(
                    signal =
                        DynamicScheduleAttentionSignal(
                            itemId = itemId,
                            schedulePriority = 0.8,
                            globalMemoryFactor = 0.2,
                            contextMemoryFactor = 0.5,
                            constraints =
                                listOf(
                                    DynamicScheduleAttentionConstraint(
                                        type =
                                            DynamicScheduleAttentionConstraintType
                                                .REQUIRED_DEPENDENCY,
                                        strength = 1.0,
                                        reason =
                                            "Required item missing",
                                    ),
                                ),
                        ),
                ),
            )

        assertEquals(
            0.75,
            result.resolvedPriority,
            0.0001,
        )

        assertTrue(
            result.conflict != null,
        )

        assertEquals(
            DynamicScheduleAttentionConflictType
                .LEARNED_SUPPRESSION_VS_HARD_REQUIREMENT,
            result.conflict?.type,
        )
    }

    @Test
    fun requiredContextCanOverrideContextMemory() {

        val result =
            resolver.resolve(
                DynamicScheduleAttentionConflictInput(
                    signal =
                        DynamicScheduleAttentionSignal(
                            itemId = itemId,
                            schedulePriority = 0.8,
                            globalMemoryFactor = 0.5,
                            contextMemoryFactor = 0.2,
                            constraints =
                                listOf(
                                    DynamicScheduleAttentionConstraint(
                                        type =
                                            DynamicScheduleAttentionConstraintType
                                                .REQUIRED_CONTEXT,
                                        strength = 1.0,
                                        reason =
                                            "Must be handled in classroom",
                                    ),
                                ),
                        ),
                ),
            )

        assertEquals(
            0.75,
            result.resolvedPriority,
            0.0001,
        )

        assertEquals(
            DynamicScheduleAttentionConflictType
                .CONTEXT_MEMORY_CONFLICT,
            result.conflict?.type,
        )
    }

    @Test
    fun explicitUserRequestReachesFullPriority() {

        val result =
            resolver.resolve(
                DynamicScheduleAttentionConflictInput(
                    signal =
                        DynamicScheduleAttentionSignal(
                            itemId = itemId,
                            schedulePriority = 0.2,
                            globalMemoryFactor = 0.1,
                            contextMemoryFactor = 0.1,
                            constraints =
                                listOf(
                                    DynamicScheduleAttentionConstraint(
                                        type =
                                            DynamicScheduleAttentionConstraintType
                                                .EXPLICIT_USER_REQUEST,
                                        strength = 1.0,
                                        reason =
                                            "User explicitly asked to remind me",
                                    ),
                                ),
                        ),
                ),
            )

        assertEquals(
            1.0,
            result.resolvedPriority,
        )

        assertTrue(
            result.attentionRequired,
        )

        assertEquals(
            DynamicScheduleAttentionConflictType
                .EXPLICIT_REQUEST_OVERRIDE,
            result.conflict?.type,
        )
    }

    @Test
    fun weakHardConstraintProducesProportionalFloor() {

        val result =
            resolver.resolve(
                DynamicScheduleAttentionConflictInput(
                    signal =
                        DynamicScheduleAttentionSignal(
                            itemId = itemId,
                            schedulePriority = 0.2,
                            globalMemoryFactor = 0.2,
                            contextMemoryFactor = 0.5,
                            constraints =
                                listOf(
                                    DynamicScheduleAttentionConstraint(
                                        type =
                                            DynamicScheduleAttentionConstraintType
                                                .REQUIRED_DEPENDENCY,
                                        strength = 0.5,
                                        reason =
                                            "Dependency has moderate confidence",
                                    ),
                                ),
                        ),
                ),
            )

        assertEquals(
            0.375,
            result.resolvedPriority,
            0.0001,
        )
    }
}
