package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult

/**
 * 12.27
 *
 * 真正执行 Historical Runtime Replay 的 Engine。
 */
interface DynamicScheduleHistoricalRuntimeReplayEngine {

    fun replay(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,
    ): DynamicScheduleIntelligenceResult
}
