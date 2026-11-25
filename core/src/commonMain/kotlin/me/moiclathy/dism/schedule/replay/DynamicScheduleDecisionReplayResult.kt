package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord

data class DynamicScheduleDecisionReplayResult(
    val originalRecord:
        DynamicScheduleDecisionTraceRecord,

    val differences:
        List<DynamicScheduleDecisionReplayDifference>,

    val equivalent: Boolean,
) {

    val changed: Boolean
        get() = !equivalent

    val priorityChanged: Boolean
        get() = differences.any {
            it.type ==
                DynamicScheduleDecisionReplayDifferenceType
                    .PRIORITY_CHANGED
        }

    val attentionDecisionChanged: Boolean
        get() = differences.any {
            it.type ==
                DynamicScheduleDecisionReplayDifferenceType
                    .ATTENTION_REQUIRED_CHANGED
        }
}
