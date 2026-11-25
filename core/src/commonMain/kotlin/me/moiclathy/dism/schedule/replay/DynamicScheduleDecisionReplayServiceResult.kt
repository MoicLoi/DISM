package me.moiclathy.dism.schedule.replay

data class DynamicScheduleDecisionReplayServiceResult(
    val report: DynamicScheduleDecisionReplayReport,
    val replayedFromSnapshot: Boolean = true,
) {
    val equivalent: Boolean
        get() = report.equivalent

    val behaviorChanged: Boolean
        get() = report.behaviorChanged
}
