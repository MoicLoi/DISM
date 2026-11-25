package me.moiclathy.dism.schedule.replay

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Instant

import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult
import me.moiclathy.dism.schedule.model.DynamicScheduleId
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.model.DynamicScheduleItemId
import me.moiclathy.dism.schedule.model.DynamicScheduleItemType
import me.moiclathy.dism.schedule.model.DynamicScheduleSource
import me.moiclathy.dism.schedule.model.DynamicScheduleState
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTrace
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage

class DeterministicDynamicScheduleDecisionReplayEngineTest {

    private val engine =
        DeterministicDynamicScheduleDecisionReplayEngine()

    private val item =
        DynamicScheduleItem(
            id =
                DynamicScheduleItemId("item-1"),

            scheduleId =
                DynamicScheduleId("schedule-1"),

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

    private fun record(
        priority: Double,
        selected: Boolean,
        state: DynamicScheduleState =
            DynamicScheduleState.READY,
        conflict: Boolean = false,
    ): DynamicScheduleDecisionTraceRecord {

        val trace =
            DynamicScheduleDecisionTrace(
                itemId = item.id,
            )
                .withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage.INPUT,

                    message =
                        "Schedule item received",

                    values =
                        mapOf(
                            "state" to state.name,
                        ),
                )
                .withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .CONFLICT_RESOLUTION,

                    message =
                        "Conflict resolved",

                    values =
                        mapOf(
                            "resolvedPriority" to
                                priority.toString(),
                        ),

                    decisive = conflict,
                )
                .withEntry(
                    stage =
                        DynamicScheduleDecisionTraceStage
                            .ATTENTION_SELECTION,

                    message =
                        "Attention selection completed",

                    values =
                        mapOf(
                            "selected" to
                                selected.toString(),
                        ),

                    decisive = selected,
                )

        return DynamicScheduleDecisionTraceRecord(
            trace = trace,

            createdAt =
                Instant.parse(
                    "2026-09-22T08:00:00Z",
                ),

            scheduleRevision = 1,
        )
    }

    @Test
    fun identicalDecisionIsEquivalent() {

        val result =
            engine.replay(
                DynamicScheduleDecisionReplayInput(
                    originalRecord =
                        record(
                            priority = 0.75,
                            selected = true,
                            conflict = true,
                        ),

                    currentItem = item,

                    currentConflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.75,
                            conflict =
                                me.moiclathy.dism.schedule.attention
                                    .DynamicScheduleAttentionConflict(
                                        itemId = item.id,
                                        type =
                                            me.moiclathy.dism.schedule.attention
                                                .DynamicScheduleAttentionConflictType
                                                .LEARNED_SUPPRESSION_VS_HARD_REQUIREMENT,
                                        originalPriority = 0.2,
                                        resolvedPriority = 0.75,
                                    ),
                            hardConstraintPresent = true,
                            attentionRequired = true,
                        ),
                ),
            )

        assertTrue(
            result.equivalent,
        )

        assertTrue(
            result.differences.isEmpty(),
        )
    }

    @Test
    fun changedPriorityIsDetected() {

        val result =
            engine.replay(
                DynamicScheduleDecisionReplayInput(
                    originalRecord =
                        record(
                            priority = 0.75,
                            selected = true,
                        ),

                    currentItem = item,

                    currentConflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.40,
                            conflict = null,
                            hardConstraintPresent = false,
                            attentionRequired = true,
                        ),
                ),
            )

        assertTrue(
            result.changed,
        )

        assertTrue(
            result.priorityChanged,
        )
    }

    @Test
    fun changedAttentionDecisionIsDetected() {

        val result =
            engine.replay(
                DynamicScheduleDecisionReplayInput(
                    originalRecord =
                        record(
                            priority = 0.75,
                            selected = true,
                        ),

                    currentItem = item,

                    currentConflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.20,
                            conflict = null,
                            hardConstraintPresent = false,
                            attentionRequired = false,
                        ),
                ),
            )

        assertTrue(
            result.attentionDecisionChanged,
        )
    }

    @Test
    fun changedItemStateIsDetected() {

        val result =
            engine.replay(
                DynamicScheduleDecisionReplayInput(
                    originalRecord =
                        record(
                            priority = 0.75,
                            selected = true,
                            state =
                                DynamicScheduleState.READY,
                        ),

                    currentItem =
                        item.copy(
                            state =
                                DynamicScheduleState.COMPLETED,
                        ),

                    currentConflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.0,
                            conflict = null,
                            hardConstraintPresent = false,
                            attentionRequired = false,
                        ),
                ),
            )

        assertTrue(
            result.differences.any {
                it.type ==
                    DynamicScheduleDecisionReplayDifferenceType
                        .ITEM_STATE_CHANGED
            },
        )
    }

    @Test
    fun conflictChangeIsDetected() {

        val result =
            engine.replay(
                DynamicScheduleDecisionReplayInput(
                    originalRecord =
                        record(
                            priority = 0.75,
                            selected = true,
                            conflict = true,
                        ),

                    currentItem = item,

                    currentConflictResult =
                        DynamicScheduleAttentionConflictResult(
                            resolvedPriority = 0.75,
                            conflict = null,
                            hardConstraintPresent = false,
                            attentionRequired = true,
                        ),
                ),
            )

        assertTrue(
            result.differences.any {
                it.type ==
                    DynamicScheduleDecisionReplayDifferenceType
                        .CONFLICT_STATE_CHANGED
            },
        )
    }
}
