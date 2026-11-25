package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService

class DeterministicDynamicScheduleDecisionReplayService(
    private val snapshotPersistence:
        DynamicScheduleDecisionSnapshotPersistenceService,

    private val replayEngine:
        DeterministicDynamicScheduleSnapshotReplayEngine,

    private val reportBuilder:
        DynamicScheduleDecisionReplayReportBuilder,

) : DynamicScheduleDecisionReplayService {

    override suspend fun replay(
        snapshotId:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshotId,
    ): DynamicScheduleDecisionReplayReport? {

        val snapshot =
            snapshotPersistence.find(
                snapshotId,
            )
                ?: return null

        /*
         * 当前这一层没有 Runtime 本身的自动重新执行入口。
         *
         * 因此这里使用 Snapshot 本身重建原始决策，
         * 并将 Snapshot 中保存的决策作为当前比较基线。
         *
         * 真正的“当前 Runtime 重跑”入口由下面的重载承担。
         */

        val originalPriority =
            snapshot.item.priority

        val originalAttentionRequired =
            snapshot.dependency.requiredSatisfied.not()

        val replayResult =
            replayEngine.replay(
                DynamicScheduleSnapshotReplayInput(
                    snapshot =
                        snapshot,

                    currentPriority =
                        originalPriority,

                    currentAttentionRequired =
                        originalAttentionRequired,

                    currentConflict =
                        null,
                ),
            )

        /*
         * 12.25 说明：ReportBuilder 的实际签名是
         * build(replay, originalTrace)，因此这里以实际签名为准。
         */
        return reportBuilder.build(
            replay =
                replayResult,

            originalTrace =
                null,
        )
    }

    override suspend fun replay(
        snapshotId:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshotId,

        currentPriority:
            Double,

        currentAttentionRequired:
            Boolean,
    ): DynamicScheduleDecisionReplayReport? {

        val snapshot =
            snapshotPersistence.find(
                snapshotId,
            )
                ?: return null

        val replayResult =
            replayEngine.replay(
                DynamicScheduleSnapshotReplayInput(
                    snapshot =
                        snapshot,

                    currentPriority =
                        currentPriority,

                    currentAttentionRequired =
                        currentAttentionRequired,

                    currentConflict =
                        null,
                ),
            )

        /*
         * 12.25 说明：ReportBuilder 的实际签名是
         * build(replay, originalTrace)，因此这里以实际签名为准。
         */
        return reportBuilder.build(
            replay =
                replayResult,

            originalTrace =
                null,
        )
    }
}
