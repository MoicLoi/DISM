package me.moiclathy.dism.schedule.replay

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId

/**
 * 13.14.4
 *
 * 批量历史重放。
 *
 * ### 职责边界（13.14.14）
 *
 * **它不负责删除历史。**
 *
 * ```text
 * Replay Batch → 读取历史 + 重新计算 + 报告结果
 * Retention    → 历史生命周期管理
 * ```
 *
 * 绝不允许出现"重放 → 校验 → 顺手删掉旧 Snapshot"这种耦合。
 *
 * ### 与设计稿的一处修正
 *
 * 设计稿在 `verify = true` 时调用
 * `verificationService.verify(snapshotId)`，而后者内部会**再重放一次**：
 *
 * ```text
 * 每条历史 → 重放两次 → 两条 Provenance
 * ```
 *
 * 所以这里用 13.13 引入的 `verify(replay)` 重载，
 * 对刚拿到的重放结果直接校验。
 *
 * ### 并发
 *
 * [DynamicScheduleHistoryReplayBatchPolicy.maxConcurrency] 真正生效：
 * 大于 1 时用协程并发执行，`Semaphore` 限制在途数量，
 * 结果**始终按输入顺序**返回（并发不改变结果的排列）。
 */
class DynamicScheduleHistoryReplayBatchService(
    private val replayService:
        DynamicScheduleHistoricalRuntimeReplayService,

    private val verificationService:
        DynamicScheduleReplayVerificationService,
) {

    suspend fun replay(
        snapshotIds:
            List<DynamicScheduleDecisionSnapshotId>,

        policy:
            DynamicScheduleHistoryReplayBatchPolicy =
            DynamicScheduleHistoryReplayBatchPolicy(),

        startedAt:
            kotlin.time.Instant =
            kotlin.time.Clock.System.now(),
    ): DynamicScheduleHistoryReplayBatchResult {

        val ids =
            snapshotIds
                .distinct()
                .take(policy.maxItems)

        /*
         * 13.15.13：整批共用同一个执行上下文。
         *
         * 批次本质上是"一次用户发起的历史重演操作"，
         * 让每条记录看起来像三次独立操作会破坏审计语义；
         * 同时也让批量重放在测试里保持确定性。
         */
        val execution =
            DynamicScheduleReplayBatchExecutionContext(
                startedAt = startedAt,

                replayEngineVersion =
                    replayService.replayEngineVersion,
            )

        if (ids.isEmpty()) {
            return DynamicScheduleHistoryReplayBatchResult(
                entries = emptyList(),
                execution = execution,
            )
        }

        val entries =
            if (policy.maxConcurrency <= 1) {

                /*
                 * 默认路径：严格顺序执行。
                 *
                 * 确定、可观察、且不会给 SQLite 制造并发压力。
                 */
                ids.map { snapshotId ->
                    replayOne(
                        snapshotId = snapshotId,
                        policy = policy,
                        execution = execution,
                    )
                }

            } else {

                runConcurrently(
                    ids = ids,
                    policy = policy,
                    execution = execution,
                )
            }

        return DynamicScheduleHistoryReplayBatchResult(
            entries = entries,
            execution = execution,
        )
    }

    private suspend fun runConcurrently(
        ids: List<DynamicScheduleDecisionSnapshotId>,

        policy: DynamicScheduleHistoryReplayBatchPolicy,

        execution: DynamicScheduleReplayBatchExecutionContext,
    ): List<DynamicScheduleHistoryReplayBatchEntry> =
        coroutineScope {

            val semaphore =
                Semaphore(permits = policy.maxConcurrency)

            ids
                .map { snapshotId ->
                    async {
                        semaphore.withPermit {
                            replayOne(
                                snapshotId = snapshotId,
                                policy = policy,
                                execution = execution,
                            )
                        }
                    }
                }
                /*
                 * awaitAll 保持输入顺序：
                 * 并发只影响"什么时候算完"，不影响结果排列。
                 */
                .awaitAll()
        }

    private suspend fun replayOne(
        snapshotId: DynamicScheduleDecisionSnapshotId,

        policy: DynamicScheduleHistoryReplayBatchPolicy,

        execution: DynamicScheduleReplayBatchExecutionContext,
    ): DynamicScheduleHistoryReplayBatchEntry {

        return try {

            val replay =
                replayService.replay(
                    snapshotId = snapshotId,

                    /*
                     * 批次内统一时间，而不是每条各取一次当前时间。
                     */
                    replayedAt = execution.startedAt,
                )

            val verification =
                if (policy.verify) {
                    verificationService.verify(
                        replay = replay,
                        verifiedAt = execution.startedAt,
                    )
                } else {
                    null
                }

            DynamicScheduleHistoryReplayBatchEntry.Success(
                snapshotId = snapshotId,
                replay = replay,
                verification = verification,
            )

        } catch (exception: Throwable) {

            /*
             * continueOnFailure = false 时由调用方（coroutineScope）
             * 负责中断整个批次，这里必须把异常放出去。
             */
            if (!policy.continueOnFailure) {
                throw exception
            }

            DynamicScheduleHistoryReplayBatchEntry.Failure(
                snapshotId = snapshotId,

                message =
                    exception.message
                        ?: exception::class
                            .simpleName
                            .orEmpty(),
            )
        }
    }
}
