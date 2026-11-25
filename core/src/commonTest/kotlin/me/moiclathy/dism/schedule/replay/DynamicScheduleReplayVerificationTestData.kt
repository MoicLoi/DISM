package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleItemSnapshot
import me.moiclathy.dism.schedule.trace.DeterministicDynamicScheduleDecisionTraceBuilder
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceRecord
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityResult

/**
 * 12.29
 *
 * Verification Test Data。
 *
 * 与生产 Runtime 解耦：
 *
 * - 历史 Snapshot 与 Replay Input 来自 [DynamicScheduleReplayTestData]
 * - Replayed Result 通过真实 Runtime 执行一次得到
 *
 * 这样 Verification 测试比较的是**真实**的
 * Historical Snapshot 与真实 Runtime Result，
 * 而不是手写出来的假对象。
 */
object DynamicScheduleReplayVerificationTestData {

    /**
     * 历史决策与重新执行结果完全一致。
     */
    fun exactMatch():
        DynamicScheduleHistoricalRuntimeReplayResult {

        val input =
            DynamicScheduleReplayTestData.create()

        return DynamicScheduleHistoricalRuntimeReplayResult(
            historicalSnapshot =
                input.decisionSnapshot,

            /*
             * 历史 Trace 必须包含 EXPLANATION 阶段，
             * 否则 Verification 会认为
             * "历史无解释 / 重放有解释" 而报 BEHAVIOR_CHANGED。
             */
            historicalTrace =
                DeterministicDynamicScheduleDecisionTraceBuilder()
                    .create(
                        input
                            .decisionSnapshot
                            .itemId,
                    )
                    .withEntry(
                        stage =
                            DynamicScheduleDecisionTraceStage
                                .EXPLANATION,

                        message =
                            "Historical decision produced an explanation.",
                    )
                    .let { trace ->
                        DynamicScheduleDecisionTraceRecord(
                            trace = trace,
                            createdAt = input.evaluatedAt,
                            scheduleRevision =
                                input.scheduleRevision,
                            engineVersion =
                                input.engineVersion,
                        )
                    },

            replayedResult =
                replay(input),

            replayInput = input,

            execution =
                DynamicScheduleReplayExecutionContext(
                    replayedAt = input.evaluatedAt,
                    executorVersion = input.engineVersion,
                ),

            /*
             * 13.10：手写 fixture 也必须对版本兼容性表态。
             * 这里历史版本与当前版本相同，所以是 EXACT。
             */
            versionCompatibility =
                DynamicScheduleHistoryVersionCompatibilityResult
                    .exact(
                        DynamicScheduleHistoryEngineVersion(
                            input.engineVersion,
                        ),
                    ),

            historicalInputComplete = true,
        )
    }

    /**
     * 历史 priority 与重新执行结果不同。
     */
    fun priorityChanged():
        DynamicScheduleHistoricalRuntimeReplayResult {

        val base =
            exactMatch()

        val changedSnapshot =
            base.historicalSnapshot.copy(
                item =
                    DynamicScheduleItemSnapshot(
                        itemId =
                            base.historicalSnapshot
                                .item
                                .itemId,

                        title =
                            base.historicalSnapshot
                                .item
                                .title,

                        state =
                            base.historicalSnapshot
                                .item
                                .state,

                        priority = 0.1,

                        confidence =
                            base.historicalSnapshot
                                .item
                                .confidence,
                    ),
            )

        return base.copy(
            historicalSnapshot = changedSnapshot,
        )
    }

    /**
     * 历史 Replay Input 不完整。
     */
    fun incomplete():
        DynamicScheduleHistoricalRuntimeReplayResult {

        return exactMatch()
            .copy(
                historicalInputComplete = false,
            )
    }

    private fun replay(
        input:
            DynamicScheduleDecisionReplayInputSnapshot,
    ) =
        DynamicScheduleReplayTestData
            .runtime()
            .evaluate(
                input = input.input,

                previous = input.previousState,

                now = input.evaluatedAt,

                policy = input.policy,
            )
}
