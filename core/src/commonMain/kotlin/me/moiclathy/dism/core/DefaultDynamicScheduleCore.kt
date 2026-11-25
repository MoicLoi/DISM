package me.moiclathy.dism.core

import kotlin.time.Instant

import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayResult
import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayService
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationReport
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationService
import me.moiclathy.dism.schedule.runtime.DynamicScheduleAttentionFeedback
import me.moiclathy.dism.schedule.runtime.DynamicScheduleDecisionRuntimeFacade
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceInput
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimePolicy
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceRuntimeState
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * DynamicScheduleCore 默认实现。
 *
 * 13.1 的作用：
 *
 * 将内部 Runtime / Replay / Verification
 * 收敛到一个公共边界。
 */
class DefaultDynamicScheduleCore(
    private val runtimeFacade:
        DynamicScheduleDecisionRuntimeFacade,

    private val historicalReplayService:
        DynamicScheduleHistoricalRuntimeReplayService,

    private val replayVerificationService:
        DynamicScheduleReplayVerificationService,
) : DynamicScheduleCore {

    override suspend fun evaluate(
        input:
            DynamicScheduleIntelligenceInput,

        previous:
            DynamicScheduleIntelligenceRuntimeState,

        now:
            Instant,

        policy:
            DynamicScheduleIntelligenceRuntimePolicy,
    ):
        DynamicScheduleIntelligenceResult {

        /*
         * 13.1 修正：
         *
         * Runtime Facade 的实际入口是 evaluateAndPersist()，
         * 它同时产出 evaluation.intelligence。
         */
        return runtimeFacade
            .evaluateAndPersist(
                input = input,

                previous = previous,

                now = now,

                policy = policy,
            )
            .intelligence
    }

    override fun applyFeedback(
        state:
            DynamicScheduleIntelligenceRuntimeState,

        feedback:
            DynamicScheduleAttentionFeedback,
    ):
        DynamicScheduleIntelligenceRuntimeState {

        return runtimeFacade.applyFeedback(
            state = state,

            feedback = feedback,
        )
    }

    override suspend fun replay(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,

        replayedAt:
            Instant,
    ):
        DynamicScheduleHistoricalRuntimeReplayResult {

        return historicalReplayService.replay(
            snapshotId = snapshotId,
            replayedAt = replayedAt,
        )
    }

    override suspend fun verifyReplay(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,

        replayedAt:
            Instant,

        verifiedAt:
            Instant,
    ):
        DynamicScheduleReplayVerificationReport {

        return replayVerificationService.verify(
            snapshotId = snapshotId,
            replayedAt = replayedAt,
            verifiedAt = verifiedAt,
        )
    }
}
