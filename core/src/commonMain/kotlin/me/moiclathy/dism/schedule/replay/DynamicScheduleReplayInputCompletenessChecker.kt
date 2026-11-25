package me.moiclathy.dism.schedule.replay

interface DynamicScheduleReplayInputCompletenessChecker {

    fun check(
        input: DynamicScheduleDecisionReplayInputSnapshot,
    ): DynamicScheduleReplayInputCompleteness
}
