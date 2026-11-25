package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.provenance.DynamicScheduleReplayVerificationProvenance
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 12.28
 *
 * Replay Verification Service。
 */
class DynamicScheduleReplayVerificationService(
    private val replayService:
        DynamicScheduleHistoricalRuntimeReplayService,

    private val verificationEngine:
        DynamicScheduleReplayVerificationEngine =
        DeterministicDynamicScheduleReplayVerificationEngine(),

    private val policy:
        DynamicScheduleReplayVerificationPolicy =
        DynamicScheduleReplayVerificationPolicy(),

    private val replayEngineVersion:
        String = "12.28",

    /**
     * 13.12.14：Verification 晚于 Replay，
     * 因此验证完成后把状态回填到这条来源链记录上。
     */
    private val provenanceStore:
        DynamicScheduleDecisionProvenanceStore? = null,
) {

    /**
     * 13.15：重放并校验。
     *
     * 两个时间都必须由调用方给出，含义完全不同：
     *
     * ```text
     * replayedAt  这次重放什么时候执行
     * verifiedAt  这次校验什么时候执行
     * ```
     */
    suspend fun verify(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,

        replayedAt:
            kotlin.time.Instant =
            kotlin.time.Clock.System.now(),

        verifiedAt:
            kotlin.time.Instant =
            kotlin.time.Clock.System.now(),
    ):
        DynamicScheduleReplayVerificationReport {

        return try {

            verify(
                replay =
                    replayService.replay(
                        snapshotId = snapshotId,
                        replayedAt = replayedAt,
                    ),

                verifiedAt = verifiedAt,
            )

        } catch (
            exception: Throwable,
        ) {

            DynamicScheduleReplayVerificationReport(
                snapshotId =
                    snapshotId.toString(),

                status =
                    DynamicScheduleReplayVerificationStatus
                        .VERIFICATION_FAILED,

                historicalEngineVersion =
                    "unknown",

                replayEngineVersion =
                    replayEngineVersion,

                historicalEvaluatedAt =
                    kotlin.time.Instant.DISTANT_PAST,

                replayedAt =
                    replayedAt,

                verifiedAt = verifiedAt,

                differences =
                    emptyList(),

                diagnostics =
                    listOf(
                        exception.message
                            ?: exception::class
                                .simpleName
                                .orEmpty(),
                    ),
            )
        }
    }

    /**
     * 13.13：对**已经算出来的**重放结果做校验。
     *
     * 为什么需要这个重载：
     *
     * [verify] 的 snapshotId 版本内部会自己重放一次。
     * 如果调用方（例如统一历史查询）为了拿到重放结果已经
     * 重放过一次，再调 snapshotId 版本就会
     *
     * ```text
     * 同一条历史 → 重放两次
     *            → 两条 Provenance 记录
     *            → 一次查询做两倍的 Runtime 工作
     * ```
     *
     * 所以把"校验一个给定结果"和"重放并校验"分开，
     * 让调用方自己决定重放几次。
     */
    suspend fun verify(
        replay:
            DynamicScheduleHistoricalRuntimeReplayResult,

        verifiedAt:
            kotlin.time.Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleReplayVerificationReport {

        val report =
            verificationEngine.verify(
                replay = replay,

                replayEngineVersion = replayEngineVersion,

                policy = policy,

                verifiedAt = verifiedAt,
            )

        /*
         * 回填验证状态。
         *
         * 回填失败不能改变验证结论本身——
         * 验证算出来的报告仍然有效，
         * 所以这里吞掉 Provenance 层的异常。
         */
        val provenanceId = replay.provenanceId

        if (provenanceId != null) {
            runCatching {
                provenanceStore?.updateVerification(
                    id = provenanceId,

                    verification =
                        DynamicScheduleReplayVerificationProvenance
                            .from(report),
                )
            }
        }

        return report
    }
}
