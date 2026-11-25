package me.moiclathy.dism.core

import me.moiclathy.dism.schedule.replay.DynamicScheduleHistoricalRuntimeReplayService
import me.moiclathy.dism.schedule.replay.DynamicScheduleReplayVerificationService
import me.moiclathy.dism.schedule.runtime.DynamicScheduleDecisionRuntimeFacade

/**
 * 13.1
 *
 * Core Factory。
 *
 * 上层模块不需要知道：
 *
 * - Runtime 具体实现
 * - Replay Service 具体实现
 * - Verification Engine 具体实现
 *
 * 这些都在 Core Boundary 后面。
 */
object DynamicScheduleCoreFactory {

    fun create(
        runtimeFacade:
            DynamicScheduleDecisionRuntimeFacade,

        historicalReplayService:
            DynamicScheduleHistoricalRuntimeReplayService,

        replayVerificationService:
            DynamicScheduleReplayVerificationService,
    ):
        DynamicScheduleCore {

        return DefaultDynamicScheduleCore(
            runtimeFacade =
                runtimeFacade,

            historicalReplayService =
                historicalReplayService,

            replayVerificationService =
                replayVerificationService,
        )
    }
}
