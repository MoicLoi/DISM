package me.moiclathy.dism.schedule.replay

import kotlin.time.Instant

/**
 * 12.28 / 13.15
 *
 * Replay Verification Engine。
 *
 * 13.15：增加 [verifiedAt] 入参。
 *
 * 时钟**不在这里**。Engine 保持纯函数：
 *
 * ```text
 * f(replay, engineVersion, policy, verifiedAt) = report
 * ```
 *
 * 同样入参必须得到同样报告，否则 12.29 的
 * Determinism 在验证层就漏掉了。
 */
interface DynamicScheduleReplayVerificationEngine {

    fun verify(
        replay:
            DynamicScheduleHistoricalRuntimeReplayResult,

        replayEngineVersion:
            String,

        policy:
            DynamicScheduleReplayVerificationPolicy =
            DynamicScheduleReplayVerificationPolicy(),

        verifiedAt:
            Instant = Instant.DISTANT_PAST,
    ): DynamicScheduleReplayVerificationReport
}
