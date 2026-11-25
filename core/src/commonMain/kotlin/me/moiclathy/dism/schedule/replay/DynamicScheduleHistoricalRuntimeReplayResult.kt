package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityResult

/**
 * 12.28 / 13.10 / 13.15
 *
 * ### 13.15：`replayedAt` / `replayEngineVersion` 收进 [execution]
 *
 * 以前这两个字段平铺在结果里，而它们描述的是**同一件事**：
 * "这一次是谁、在什么时候跑的"。
 *
 * 平铺会造成两个来源：
 *
 * ```text
 * execution.replayedAt   ← 执行上下文的事实
 * result.replayedAt      ← 结果里的副本
 * ```
 *
 * 一旦两者不同步就无法解释。所以只保留 [execution] 作为唯一来源，
 * 并用只读访问器 [replayedAt] / [replayEngineVersion] 保持兼容。
 */
data class DynamicScheduleHistoricalRuntimeReplayResult(
    val historicalSnapshot:
        DynamicScheduleDecisionSnapshot,

    val historicalTrace:
        DynamicScheduleDecisionTraceRecord?,

    val replayedResult:
        DynamicScheduleIntelligenceResult,

    val replayInput:
        DynamicScheduleDecisionReplayInputSnapshot,

    /**
     * 13.15：本次重放的执行上下文。
     *
     * ```text
     * execution.replayedAt    = 现在什么时候重新运行
     * replayInput.evaluatedAt = 历史 Runtime 当时什么时候评估
     * ```
     *
     * 这两个时间**必须**是分开的两件事。
     */
    val execution:
        DynamicScheduleReplayExecutionContext,

    /**
     * 13.10.13：这次重放所依据的版本兼容性判断。
     *
     * 于是 Replay 结果不再只是"一个结果对象"，而是：
     *
     * ```text
     * historical version
     * current version
     * compatibility
     * replayed result
     * ```
     *
     * 调用方因此能区分 EXACT 与 REPLAY_COMPATIBLE——
     * 后者意味着"结果可用，但当时的引擎语义与现在不完全相同"。
     */
    val versionCompatibility:
        DynamicScheduleHistoryVersionCompatibilityResult,

    /**
     * 13.12.15：本次重放留下的来源链记录 id。
     *
     * UI / 日志只需要这一个 ID，
     * 就能顺着查回 Snapshot / Trace / ReplayInput / 版本路径 / 验证状态。
     *
     * 为 null 表示这次重放没有落 Provenance
     * （例如调用方注入的 Replay Service 没有配 Provenance Store）。
     */
    val provenanceId:
        me.moiclathy.dism.schedule.provenance
            .DynamicScheduleDecisionProvenanceId? = null,

    val historicalInputComplete:
        Boolean = true,
) {

    /**
     * 本次 Replay 的执行时间。唯一来源仍是 [execution]。
     */
    val replayedAt: kotlin.time.Instant
        get() = execution.replayedAt

    /**
     * 实际执行这次 Replay 的引擎版本。唯一来源仍是 [execution]。
     */
    val replayEngineVersion: String
        get() = execution.executorVersion

    /**
     * 历史评估时间。与 [replayedAt] 是两件事。
     */
    val historicalEvaluatedAt: kotlin.time.Instant
        get() = replayInput.evaluatedAt
}
