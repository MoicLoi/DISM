package me.moiclathy.dism.core

import kotlin.time.Instant

import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationReport
import me.moiclathy.dism.schedule.runtime.DynamicScheduleAttentionFeedback
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * DISM Core 的稳定公共入口。
 *
 * 13.1：
 *
 * 上层模块原则上只依赖这个接口，
 * 不直接依赖 Runtime 内部组件。
 *
 * App / Server / CLI
 *        ↓
 * DynamicScheduleCore
 *        ↓
 * Schedule Intelligence
 */
interface DynamicScheduleCore {

    /**
     * 执行一次 Dynamic Schedule Intelligence。
     *
     * 13.1 修正：该入口经由 Runtime Facade，
     * 会同时持久化 Snapshot / Trace / Replay Input，
     * 因此是 suspend。
     */
    suspend fun evaluate(
        input:
            DynamicScheduleIntelligenceInput,

        previous:
            DynamicScheduleIntelligenceRuntimeState =
            DynamicScheduleIntelligenceRuntimeState(),

        now:
            Instant,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy =
            DynamicScheduleIntelligenceRuntimePolicy(),
    ):
        DynamicScheduleIntelligenceResult

    /**
     * 将用户反馈应用到 Runtime State。
     */
    fun applyFeedback(
        state:
            DynamicScheduleIntelligenceRuntimeState,

        feedback:
            DynamicScheduleAttentionFeedback,
    ):
        DynamicScheduleIntelligenceRuntimeState

    /**
     * 对已经持久化的历史决策执行真正的 Historical Replay。
     *
     * 13.15：[replayedAt] 是**这次重放的执行时间**，
     * 与历史评估时间无关。默认为当前时间；
     * 测试传入固定时间即可保持确定性。
     */
    suspend fun replay(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,

        replayedAt:
            Instant =
            kotlin.time.Clock.System.now(),
    ):
        DynamicScheduleHistoricalRuntimeReplayResult

    /**
     * 对历史 Replay 执行 Verification。
     *
     * 13.15：[replayedAt] 与 [verifiedAt] 是两条时间线——
     * "重放跑完了"和"有人校验了结果"可能相隔很久。
     */
    suspend fun verifyReplay(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,

        replayedAt:
            Instant =
            kotlin.time.Clock.System.now(),

        verifiedAt:
            Instant =
            kotlin.time.Clock.System.now(),
    ):
        DynamicScheduleReplayVerificationReport
}
