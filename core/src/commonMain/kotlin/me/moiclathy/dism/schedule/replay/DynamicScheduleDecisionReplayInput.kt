package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult
import me.moiclathy.dism.schedule.model.DynamicScheduleItem
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

data class DynamicScheduleDecisionReplayInput(
    val originalRecord: DynamicScheduleDecisionTraceRecord,

    val currentItem: DynamicScheduleItem,

    val currentConflictResult:
        DynamicScheduleAttentionConflictResult,
)
