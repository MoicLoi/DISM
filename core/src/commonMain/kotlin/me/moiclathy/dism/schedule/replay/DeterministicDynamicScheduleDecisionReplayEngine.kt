package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

class DeterministicDynamicScheduleDecisionReplayEngine :
    DynamicScheduleDecisionReplayEngine {

    override fun replay(
        input: DynamicScheduleDecisionReplayInput,
    ): DynamicScheduleDecisionReplayResult {

        val original =
            input.originalRecord

        val current =
            input.currentConflictResult

        val differences =
            mutableListOf<
                DynamicScheduleDecisionReplayDifference
            >()

        comparePriority(
            original = original,
            current = current,
            differences = differences,
        )

        compareAttentionDecision(
            original = original,
            current = current,
            differences = differences,
        )

        compareConflictState(
            original = original,
            current = current,
            differences = differences,
        )

        compareScheduleRevision(
            original = original,
            currentItem = input.currentItem,
            differences = differences,
        )

        compareItemState(
            original = original,
            currentItem = input.currentItem,
            differences = differences,
        )

        return DynamicScheduleDecisionReplayResult(
            originalRecord = original,
            differences = differences,
            equivalent = differences.isEmpty(),
        )
    }

    private fun comparePriority(
        original:
            DynamicScheduleDecisionTraceRecord,

        current:
            DynamicScheduleAttentionConflictResult,

        differences:
            MutableList<
                DynamicScheduleDecisionReplayDifference
            >,
    ) {

        val originalPriority =
            original.trace.entries
                .firstOrNull {
                    it.stage ==
                        me.moiclathy.dism.schedule.trace
                            .DynamicScheduleDecisionTraceStage
                            .CONFLICT_RESOLUTION
                }
                ?.values
                ?.get("resolvedPriority")

        if (originalPriority == null) {
            return
        }

        val currentPriority =
            current.resolvedPriority
                .toString()

        if (originalPriority != currentPriority) {

            differences +=
                DynamicScheduleDecisionReplayDifference(
                    type =
                        DynamicScheduleDecisionReplayDifferenceType
                            .PRIORITY_CHANGED,

                    originalValue =
                        originalPriority,

                    currentValue =
                        currentPriority,

                    message =
                        "Resolved attention priority changed",
                )
        }
    }

    private fun compareAttentionDecision(
        original:
            DynamicScheduleDecisionTraceRecord,

        current:
            DynamicScheduleAttentionConflictResult,

        differences:
            MutableList<
                DynamicScheduleDecisionReplayDifference
            >,
    ) {

        val originalSelected =
            original.trace.entries
                .firstOrNull {
                    it.stage ==
                        me.moiclathy.dism.schedule.trace
                            .DynamicScheduleDecisionTraceStage
                            .ATTENTION_SELECTION
                }
                ?.values
                ?.get("selected")
                ?.toBooleanStrictOrNull()

        if (originalSelected == null) {
            return
        }

        if (
            originalSelected !=
                current.attentionRequired
        ) {

            differences +=
                DynamicScheduleDecisionReplayDifference(
                    type =
                        DynamicScheduleDecisionReplayDifferenceType
                            .ATTENTION_REQUIRED_CHANGED,

                    originalValue =
                        originalSelected.toString(),

                    currentValue =
                        current.attentionRequired.toString(),

                    message =
                        "Attention decision changed",
                )
        }
    }

    private fun compareConflictState(
        original:
            DynamicScheduleDecisionTraceRecord,

        current:
            DynamicScheduleAttentionConflictResult,

        differences:
            MutableList<
                DynamicScheduleDecisionReplayDifference
            >,
    ) {

        val originalConflict =
            original.trace.entries.any {
                it.stage ==
                    me.moiclathy.dism.schedule.trace
                        .DynamicScheduleDecisionTraceStage
                        .CONFLICT_RESOLUTION &&
                    it.decisive
            }

        val currentConflict =
            current.conflict != null

        if (
            originalConflict !=
                currentConflict
        ) {

            differences +=
                DynamicScheduleDecisionReplayDifference(
                    type =
                        DynamicScheduleDecisionReplayDifferenceType
                            .CONFLICT_STATE_CHANGED,

                    originalValue =
                        originalConflict.toString(),

                    currentValue =
                        currentConflict.toString(),

                    message =
                        "Conflict resolution state changed",
                )
        }
    }

    private fun compareScheduleRevision(
        original:
            DynamicScheduleDecisionTraceRecord,

        currentItem:
            DynamicScheduleItem,

        differences:
            MutableList<
                DynamicScheduleDecisionReplayDifference
            >,
    ) {

        /*
         * 当前 DynamicScheduleItem 本身没有 revision。
         *
         * 因此这里只保留接口位置，
         * 真正的 revision comparison 在后续
         * Schedule Snapshot 接入后完成。
         */
    }

    private fun compareItemState(
        original:
            DynamicScheduleDecisionTraceRecord,

        currentItem:
            DynamicScheduleItem,

        differences:
            MutableList<
                DynamicScheduleDecisionReplayDifference
            >,
    ) {

        val originalState =
            original.trace.entries
                .firstOrNull {
                    it.stage ==
                        me.moiclathy.dism.schedule.trace
                            .DynamicScheduleDecisionTraceStage
                            .INPUT
                }
                ?.values
                ?.get("state")

        if (originalState == null) {
            return
        }

        val currentState =
            currentItem.state.name

        if (originalState != currentState) {

            differences +=
                DynamicScheduleDecisionReplayDifference(
                    type =
                        DynamicScheduleDecisionReplayDifferenceType
                            .ITEM_STATE_CHANGED,

                    originalValue =
                        originalState,

                    currentValue =
                        currentState,

                    message =
                        "Schedule item state changed",
                )
        }
    }
}
