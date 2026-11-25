package me.moiclathy.dism.schedule.attention

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState

class DeterministicDynamicScheduleAttentionExplanationEngineTest {

    private val item =
        DynamicScheduleItem(
            id =
                DynamicScheduleItemId(
                    "item-1",
                ),
            scheduleId =
                DynamicScheduleId(
                    "schedule-1",
                ),
            title = "实验课",
            type =
                DynamicScheduleItemType.EVENT,
            source =
                DynamicScheduleSource.DERIVED,
            state =
                DynamicScheduleState.READY,
            priority = 0.8,
            confidence = 1.0,
        )

    private val engine =
        DeterministicDynamicScheduleAttentionExplanationEngine()

    @Test
    fun normalAttentionProducesSummary() {

        val result =
            engine.explain(
                DynamicScheduleAttentionExplanationInput(
                    item = item,

                    conflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.8,
                            conflict = null,
                            hardConstraintPresent = false,
                            attentionRequired = true,
                        ),
                ),
            )

        assertTrue(
            result.summary.contains("实验课"),
        )

        assertTrue(
            result.reasons.isNotEmpty(),
        )
    }

    @Test
    fun dependencyOverrideIsExplained() {

        val constraint =
            DynamicScheduleAttentionConstraint(
                type =
                    DynamicScheduleAttentionConstraintType
                        .REQUIRED_DEPENDENCY,
                strength = 1.0,
                reason =
                    "需要校园卡，但当前校园卡不在身上",
            )

        val conflict =
            DynamicScheduleAttentionConflict(
                itemId = item.id,
                type =
                    DynamicScheduleAttentionConflictType
                        .LEARNED_SUPPRESSION_VS_HARD_REQUIREMENT,
                originalPriority = 0.2,
                resolvedPriority = 0.75,
                overriddenBy = constraint,
                reasons =
                    listOf(
                        "Hard constraint overrides learned suppression",
                    ),
            )

        val result =
            engine.explain(
                DynamicScheduleAttentionExplanationInput(
                    item = item,
                    conflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.75,
                            conflict = conflict,
                            hardConstraintPresent = true,
                            attentionRequired = true,
                        ),
                ),
            )

        assertTrue(
            result.summary.contains(
                "required condition",
            ),
        )

        assertTrue(
            result.decisiveReasons.any {
                it.type ==
                    DynamicScheduleAttentionExplanationReasonType
                        .REQUIRED_DEPENDENCY
            },
        )
    }

    @Test
    fun explicitRequestGetsHighestPriorityExplanation() {

        val constraint =
            DynamicScheduleAttentionConstraint(
                type =
                    DynamicScheduleAttentionConstraintType
                        .EXPLICIT_USER_REQUEST,
                strength = 1.0,
                reason =
                    "用户明确要求提醒",
            )

        val conflict =
            DynamicScheduleAttentionConflict(
                itemId = item.id,
                type =
                    DynamicScheduleAttentionConflictType
                        .EXPLICIT_REQUEST_OVERRIDE,
                originalPriority = 0.1,
                resolvedPriority = 1.0,
                overriddenBy = constraint,
            )

        val result =
            engine.explain(
                DynamicScheduleAttentionExplanationInput(
                    item = item,
                    conflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 1.0,
                            conflict = conflict,
                            hardConstraintPresent = true,
                            attentionRequired = true,
                        ),
                ),
            )

        assertTrue(
            result.summary.contains(
                "explicitly asked",
            ),
        )

        assertEquals(
            DynamicScheduleAttentionExplanationReasonType
                .EXPLICIT_USER_REQUEST,
            result.decisiveReasons.first().type,
        )
    }

    @Test
    fun explanationRespectsAttentionBudget() {

        val result =
            engine.explain(
                DynamicScheduleAttentionExplanationInput(
                    item = item,

                    conflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.8,
                            conflict = null,
                            hardConstraintPresent = false,
                            attentionRequired = true,
                        ),

                    contextReasons =
                        listOf(
                            "Context A",
                            "Context B",
                            "Context C",
                            "Context D",
                            "Context E",
                        ),

                    adaptationReasons =
                        listOf(
                            "Memory A",
                            "Memory B",
                        ),
                ),
                policy =
                    DynamicScheduleAttentionExplanationPolicy(
                        maximumReasons = 3,
                    ),
            )

        assertEquals(
            3,
            result.reasons.size,
        )
    }
}
