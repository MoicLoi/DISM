package me.moiclathy.dism.schedule.replay

enum class DynamicScheduleDecisionReplayDifferenceType {

    PRIORITY_CHANGED,

    ATTENTION_REQUIRED_CHANGED,

    CONFLICT_STATE_CHANGED,

    SCHEDULE_REVISION_CHANGED,

    ITEM_STATE_CHANGED,

    NO_DIFFERENCE,
}

data class DynamicScheduleDecisionReplayDifference(
    val type: DynamicScheduleDecisionReplayDifferenceType,

    val originalValue: String,

    val currentValue: String,

    val message: String,
)
