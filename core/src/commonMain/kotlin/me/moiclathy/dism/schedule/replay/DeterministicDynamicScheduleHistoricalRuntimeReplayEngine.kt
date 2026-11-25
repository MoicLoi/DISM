package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntime
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult

/**
 * 12.27
 *
 * Deterministic Historical Runtime Replay。
 *
 * 与 SnapshotReplayEngine 的区别：
 *
 * SnapshotReplayEngine：
 *
 *     Snapshot
 *        ↓
 *     重新计算少量字段
 *
 * HistoricalRuntimeReplayEngine：
 *
 *     Historical Runtime Input
 *        ↓
 *     DynamicScheduleIntelligenceRuntime.evaluate()
 *        ↓
 *     完整 Runtime Result
 */
class DeterministicDynamicScheduleHistoricalRuntimeReplayEngine(
    /*
     * 12.27 修正：
     *
     * 真实 Runtime 需要由调用方注入
     * （attentionProjector 依赖 recommendation identity resolver，
     * 无法在库内凭空构造默认实现）。
     */
    private val runtime:
        DynamicScheduleIntelligenceRuntime,
) : DynamicScheduleHistoricalRuntimeReplayEngine {

    override fun replay(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,
    ): DynamicScheduleIntelligenceResult {

        return runtime.evaluate(
            input =
                input.input,

            previous =
                input.previousState,

            now =
                input.evaluatedAt,

            policy =
                input.policy,
        )
    }
}
