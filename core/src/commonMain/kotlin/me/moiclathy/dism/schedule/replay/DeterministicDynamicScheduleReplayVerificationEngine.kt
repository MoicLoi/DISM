package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTraceStage

/**
 * 12.28
 *
 * Deterministic Replay Verification。
 *
 * 注意：
 *
 * Verification 不重新执行 Runtime。
 *
 * Runtime Replay 已经由 12.27 完成。
 *
 * 这里负责比较：
 *
 * Historical Snapshot
 *          VS
 * Replayed Runtime Result
 */
class DeterministicDynamicScheduleReplayVerificationEngine :
    DynamicScheduleReplayVerificationEngine {

    override fun verify(
        replay:
            DynamicScheduleHistoricalRuntimeReplayResult,

        replayEngineVersion:
            String,

        policy:
            DynamicScheduleReplayVerificationPolicy,

        verifiedAt:
            kotlin.time.Instant,
    ): DynamicScheduleReplayVerificationReport {

        val historical =
            replay
                .historicalSnapshot

        val replayed =
            replay
                .replayedResult

        val differences =
            mutableListOf<
                DynamicScheduleReplayVerificationDifference
            >()

        /*
         * ---------------------------------------------------------
         * Engine Version
         *
         * 13.10 的升级点在这里。
         *
         * 以前是裸字符串比较：
         *
         * ```text
         * historical != current → ENGINE_VERSION_MISMATCH
         * ```
         *
         * 但版本号不同不等于语义不同：
         * 13.1 → 13.2 可能只是内部实现升级。
         *
         * 现在以 Compatibility Registry 的**显式判断**为准：
         *
         * - 已声明可兼容   → 不是 mismatch
         * - 未声明 / 不兼容 → 仍然是 mismatch
         *
         * 注意注册表默认对未知版本返回 UNKNOWN，
         * 所以这里不会因为放宽而漏报真正的语义变更。
         * ---------------------------------------------------------
         */

        val historicalEngineVersion =
            replay
                .replayInput
                .engineVersion

        val engineVersionMismatch =
            historicalEngineVersion !=
                replayEngineVersion &&
                !replay
                    .versionCompatibility
                    .canReplayDirectly

        /*
         * ---------------------------------------------------------
         * 找到当前 Replay 对应 Item
         * ---------------------------------------------------------
         */

        val replayedAttentionItem =
            replayed
                .attentionProjection
                .candidates
                .firstOrNull {
                    it.itemId ==
                        historical.item.itemId
                }

        /*
         * ---------------------------------------------------------
         * Priority
         * ---------------------------------------------------------
         */

        val historicalPriority =
            historical
                .item
                .priority

        val replayedPriority =
            replayedAttentionItem
                ?.candidate
                ?.recommendation
                ?.priority
                ?: historicalPriority

        val priorityChanged =
            kotlin.math.abs(
                historicalPriority -
                    replayedPriority,
            ) >
                policy.priorityTolerance

        if (priorityChanged) {

            differences +=
                DynamicScheduleReplayVerificationDifference(
                    factor = "priority",

                    historicalValue =
                        historicalPriority
                            .toString(),

                    replayedValue =
                        replayedPriority
                            .toString(),

                    description =
                        "Historical decision priority differs from replayed priority.",
                )
        }

        /*
         * ---------------------------------------------------------
         * Attention Decision
         * ---------------------------------------------------------
         */

        val historicalConflict =
            !historical
                .dependency
                .requiredSatisfied

        val replayedConflict =
            replayed
                .attentionConflicts[
                    historical.item.itemId
                ]
                ?.conflict != null

        if (
            policy.requireConflictParity &&
            historicalConflict != replayedConflict
        ) {

            differences +=
                DynamicScheduleReplayVerificationDifference(
                    factor = "conflict",

                    historicalValue =
                        historicalConflict
                            .toString(),

                    replayedValue =
                        replayedConflict
                            .toString(),

                    description =
                        "Historical conflict state differs from replayed conflict state.",
                )
        }

        /*
         * ---------------------------------------------------------
         * Explanation
         * ---------------------------------------------------------
         */

        val historicalExplanationExists =
            replay
                .historicalTrace
                ?.trace
                ?.entries
                ?.any {
                    it.stage ==
                        DynamicScheduleDecisionTraceStage.EXPLANATION
                }
                ?: false

        val replayedExplanationExists =
            replayed
                .attentionExplanations
                .containsKey(
                    historical.item.itemId,
                )

        if (
            policy.requireExplanationParity &&
            historicalExplanationExists !=
            replayedExplanationExists
        ) {

            differences +=
                DynamicScheduleReplayVerificationDifference(
                    factor = "explanation",

                    historicalValue =
                        historicalExplanationExists
                            .toString(),

                    replayedValue =
                        replayedExplanationExists
                            .toString(),

                    description =
                        "Historical explanation state differs from replayed explanation state.",
                )
        }

        /*
         * ---------------------------------------------------------
         * Attention decision
         * ---------------------------------------------------------
         *
         * 这里不再通过 Snapshot 的旧 priority 公式推算。
         *
         * Replay 已经经过完整 Runtime，
         * 所以直接使用 Runtime Projection Result。
         */

        val historicalAttentionRequired =
            historicalConflict ||
                historicalPriority >=
                historical.policy.minimumAttentionPriority

        val replayedAttentionRequired =
            replayed
                .attentionConflicts[
                    historical.item.itemId
                ]
                ?.attentionRequired
                ?: false

        if (
            historicalAttentionRequired !=
            replayedAttentionRequired
        ) {

            differences +=
                DynamicScheduleReplayVerificationDifference(
                    factor = "attention",

                    historicalValue =
                        historicalAttentionRequired
                            .toString(),

                    replayedValue =
                        replayedAttentionRequired
                            .toString(),

                    description =
                        "Historical attention decision differs from replayed attention decision.",
                )
        }

        /*
         * ---------------------------------------------------------
         * 判断最终状态
         * ---------------------------------------------------------
         */

        val status =
            when {

                !replay.historicalInputComplete ->
                    DynamicScheduleReplayVerificationStatus
                        .INPUT_INCOMPLETE

                engineVersionMismatch &&
                    policy.requireSameEngineVersion ->
                    DynamicScheduleReplayVerificationStatus
                        .ENGINE_VERSION_MISMATCH

                differences.isEmpty() ->
                    DynamicScheduleReplayVerificationStatus
                        .EXACT_MATCH

                else ->
                    DynamicScheduleReplayVerificationStatus
                        .BEHAVIOR_CHANGED
            }

        val diagnostics =
            buildList {

                if (engineVersionMismatch) {

                    add(
                        "Historical engine version " +
                            "'$historicalEngineVersion' " +
                            "differs from replay engine version " +
                            "'$replayEngineVersion'.",
                    )
                }

                if (differences.isNotEmpty()) {

                    add(
                        "Replay produced " +
                            "${differences.size} " +
                            "verification difference(s).",
                    )
                }

                if (
                    replay
                        .historicalInputComplete
                        .not()
                ) {

                    add(
                        "Historical Runtime Input was incomplete.",
                    )
                }
            }

        return DynamicScheduleReplayVerificationReport(
            snapshotId =
                historical
                    .id
                    .toString(),

            status =
                status,

            historicalEngineVersion =
                historicalEngineVersion,

            replayEngineVersion =
                replayEngineVersion,

            historicalEvaluatedAt =
                replay
                    .replayInput
                    .evaluatedAt,

            replayedAt =
                replay
                    .execution
                    .replayedAt,

            /*
             * 13.15：校验时间由调用方给出。
             *
             * Engine 不读系统时钟——它是纯函数，
             * 同样的入参必须得到同样的报告。
             */
            verifiedAt =
                verifiedAt,

            differences =
                differences,

            diagnostics =
                diagnostics,
        )
    }
}
