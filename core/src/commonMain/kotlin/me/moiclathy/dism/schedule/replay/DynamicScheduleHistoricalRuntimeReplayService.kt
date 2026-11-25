package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceBuilder
import me.moiclathy.dism.schedule.provenance.DynamicScheduleDecisionProvenanceStore
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService
import me.moiclathy.dism.schedule.version.DefaultDynamicScheduleHistoryVersionCompatibilityRegistry
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryEngineVersion
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityRegistry
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionCompatibilityResult
import me.moiclathy.dism.schedule.version.DynamicScheduleHistoryVersionDiagnostic

/**
 * 12.27
 *
 * True Historical Runtime Replay。
 */
class DynamicScheduleHistoricalRuntimeReplayService(
    private val replayInputPersistence:
        DynamicScheduleDecisionReplayInputSnapshotPersistenceService,

    private val snapshotPersistence:
        DynamicScheduleDecisionSnapshotPersistenceService,

    private val tracePersistence:
        DynamicScheduleDecisionTracePersistenceService,

    private val completenessChecker:
        DynamicScheduleReplayInputCompletenessChecker =
        DeterministicDynamicScheduleReplayInputCompletenessChecker(),

    /*
     * 12.27 修正：
     *
     * Replay Engine 依赖真实 Runtime（含 identity resolver），
     * 因此必须由调用方注入，而不是在库内构造默认实现。
     */
    private val replayEngine:
        DynamicScheduleHistoricalRuntimeReplayEngine,

    /**
     * 12.28：记录本次 replay 引擎版本，
     * 供 Replay Verification 判断版本一致性。
     *
     * 13.15：改为公开只读——批量重放需要把同一个执行者版本
     * 记进 [DynamicScheduleReplayBatchExecutionContext]。
     */
    val replayEngineVersion:
        String = "12.27",

    /**
     * 13.10：版本兼容性判断。
     *
     * 默认值只认识"版本完全相同"，
     * 其它版本一律 UNKNOWN（不允许直接重放）。
     */
    private val compatibilityRegistry:
        DynamicScheduleHistoryVersionCompatibilityRegistry =
        DefaultDynamicScheduleHistoryVersionCompatibilityRegistry(
            currentVersion = "12.27",
        ),

    /**
     * 13.12：重放成功后落一条来源链记录。
     *
     * 为 null 时只是不记录（例如纯内存的一次性重放），
     * 不影响重放结果本身。
     */
    private val provenanceStore:
        DynamicScheduleDecisionProvenanceStore? = null,

    private val provenanceBuilder:
        DynamicScheduleDecisionProvenanceBuilder =
        DynamicScheduleDecisionProvenanceBuilder(),
) {

    /**
     * 13.10：不执行重放，只回答"这条历史能不能用当前引擎重放"。
     *
     * UI 应该用这个而不是捕获异常。
     */
    suspend fun versionDiagnostic(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleHistoryVersionDiagnostic {

        val compatibility =
            resolveCompatibility(snapshotId)

        return DynamicScheduleHistoryVersionDiagnostic
            .from(compatibility)
    }

    /**
     * 13.15：真正执行一次历史重放。
     *
     * [replayedAt] 是**这次重放的执行时间**，必须由调用方给出：
     *
     * - 生产路径默认取当前时间；
     * - 测试可以传固定时间，保持确定性；
     * - 批量重放传批次统一的 `startedAt`。
     *
     * 它**绝不**等于 `replayInput.evaluatedAt`——
     * 那是"历史 Runtime 当时什么时候评估"，
     * 是另一条时间线上的事情。
     */
    suspend fun replay(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,

        replayedAt:
            kotlin.time.Instant =
            kotlin.time.Clock.System.now(),
    ):
        DynamicScheduleHistoricalRuntimeReplayResult {

        val replayInput =
            replayInputPersistence.find(
                snapshotId,
            )
                ?: error(
                    "Historical replay input not found: $snapshotId",
                )

        val completeness =
            completenessChecker.check(
                replayInput,
            )

        require(completeness.complete) {
            buildString {
                append(
                    "Historical replay input is incomplete. ",
                )

                append(
                    completeness
                        .missingInputs
                        .joinToString(),
                )
            }
        }

        /*
         * -----------------------------------------------------
         * 13.10 版本闸门
         *
         * 在读历史版本之后、真正跑 Runtime 之前判断兼容性。
         *
         * 否则就会出现最危险的情况：
         *
         * ```text
         * 旧数据
         *    ↓
         * 新 Runtime
         *    ↓
         * 默默 Replay
         * ```
         * -----------------------------------------------------
         */

        val compatibility =
            resolveCompatibility(replayInput)

        if (!compatibility.canReplayDirectly) {
            throw DynamicScheduleHistoryReplayBlockedException(
                compatibility,
            )
        }

        val snapshot =
            snapshotPersistence.find(
                snapshotId,
            )
                ?: replayInput.decisionSnapshot

        val trace =
            snapshot.traceId?.let {
                tracePersistence.find(it)
            }

        val replayed =
            replayEngine.replay(
                replayInput,
            )

        /*
         * 13.15：执行上下文是本次重放的**唯一时间来源**。
         *
         * ReplayInput.evaluatedAt 属于历史，不是现在。
         */
        val execution =
            DynamicScheduleReplayExecutionContext(
                replayedAt = replayedAt,
                executorVersion = replayEngineVersion,
            )

        /*
         * 13.12.12：重放成功之后落一条来源链记录。
         *
         * 注意 provenance 里**没有** verificationStatus——
         * Verification 可能晚于 Replay（13.12.14），
         * 那时再通过 updateVerificationStatus 回填。
         */
        /*
         * 13.16：行为指纹。
         *
         * 复用 12.29 已有的 deterministic fingerprint，
         * 不重新造一套算法，也刻意**不引入 SHA-256**——
         * 现在需要的是"可比较的 canonical 表示"，
         * 而不是压缩数据库字段。
         */
        val behaviorFingerprint =
            DynamicScheduleReplayDeterminismFingerprint
                .from(replayed)
                .canonical

        val provenance =
            provenanceStore?.let { store ->

                val record =
                    provenanceBuilder.build(
                        snapshot = snapshot,

                        historicalEngineVersion =
                            replayInput.engineVersion,

                        replayEngineVersion =
                            execution.executorVersion,

                        compatibility =
                            compatibility.compatibility,

                        migrationId = null,

                        behaviorFingerprint =
                            behaviorFingerprint,

                        /*
                         * Provenance 记录是被这次重放创建的，
                         * 所以它的创建时间就是重放时间。
                         */
                        evaluatedAt =
                            execution.replayedAt,

                        replayedAt =
                            execution.replayedAt,
                    )

                store.save(record)

                record
            }

        return DynamicScheduleHistoricalRuntimeReplayResult(
            historicalSnapshot =
                snapshot,

            historicalTrace =
                trace,

            replayedResult =
                replayed,

            replayInput =
                replayInput,

            execution = execution,

            versionCompatibility =
                compatibility,

            provenanceId = provenance?.id,

            historicalInputComplete =
                completeness.complete,
        )
    }

    private suspend fun resolveCompatibility(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleHistoryVersionCompatibilityResult {

        val replayInput =
            replayInputPersistence.find(snapshotId)
                ?: error(
                    "Historical replay input not found: $snapshotId",
                )

        return resolveCompatibility(replayInput)
    }

    private fun resolveCompatibility(
        replayInput:
            DynamicScheduleDecisionReplayInputSnapshot,
    ): DynamicScheduleHistoryVersionCompatibilityResult =
        compatibilityRegistry.resolve(
            historicalVersion =
                DynamicScheduleHistoryEngineVersion(
                    replayInput.engineVersion,
                ),

            currentVersion =
                DynamicScheduleHistoryEngineVersion(
                    replayEngineVersion,
                ),
        )

    suspend fun replayReport(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,
    ):
        DynamicScheduleHistoricalRuntimeReplayReport {

        val result =
            replay(
                snapshotId,
            )

        val difference =
            DynamicScheduleHistoricalRuntimeReplayDifference.compare(
                snapshot =
                    result.historicalSnapshot,

                replayed =
                    result.replayedResult,
            )

        return DynamicScheduleHistoricalRuntimeReplayReport(
            snapshotId =
                snapshotId.toString(),

            engineVersion =
                result
                    .replayInput
                    .engineVersion,

            evaluatedAt =
                result
                    .replayInput
                    .evaluatedAt,

            replayedAt =
                result.replayedAt,

            historicalInputComplete =
                result.historicalInputComplete,

            difference =
                difference,
        )
    }
}
