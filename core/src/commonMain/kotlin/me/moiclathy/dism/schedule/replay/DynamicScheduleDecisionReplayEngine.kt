package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult

interface DynamicScheduleDecisionReplayEngine {

    fun replay(
        input: DynamicScheduleDecisionReplayInput,
    ): DynamicScheduleDecisionReplayResult
}
