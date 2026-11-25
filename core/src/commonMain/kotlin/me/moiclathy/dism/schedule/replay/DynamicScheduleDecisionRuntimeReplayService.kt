package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.attention.DynamicScheduleAttentionConflictResult
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotId
import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshotPersistenceService
import me.moiclathy.dism.schedule.trace.DynamicScheduleDecisionTracePersistenceService

/**
 * 13.2：包边界修正。
 *
 * 这个类原本在 `schedule/runtime/`，
 * 但它 import 的全是 `schedule.replay.*`，
 * 职责也是
 *
 * ```text
 * SnapshotPersistence
 *        ↓
 * SnapshotReplayEngine
 *        ↓
 * ReplayReportBuilder
 *        ↓
 * ReplayReport
 * ```
 *
 * 所以它属于历史 Replay 层，而不是 Runtime。
 * 这样 `runtime → replay` 这条不该存在的依赖就消失了。
 */
class DynamicScheduleDecisionRuntimeReplayService(
    private val snapshotPersistence:
        DynamicScheduleDecisionSnapshotPersistenceService,

    private val tracePersistence:
        DynamicScheduleDecisionTracePersistenceService,

    private val replayEngine:
        DeterministicDynamicScheduleSnapshotReplayEngine =
        DeterministicDynamicScheduleSnapshotReplayEngine(),

    private val reportBuilder:
        DeterministicDynamicScheduleDecisionReplayReportBuilder =
        DeterministicDynamicScheduleDecisionReplayReportBuilder(),
) {

    suspend fun replay(
        snapshotId:
            DynamicScheduleDecisionSnapshotId,

        currentPriority: Double,

        currentAttentionRequired: Boolean,

        currentConflict:
            DynamicScheduleAttentionConflictResult? = null,
    ): DynamicScheduleDecisionReplayReport {

        require(currentPriority in 0.0..1.0)

        val snapshot =
            snapshotPersistence.find(
                snapshotId,
            )
                ?: error(
                    "Decision snapshot not found: $snapshotId",
                )

        val replay =
            replayEngine.replay(
                DynamicScheduleSnapshotReplayInput(
                    snapshot =
                        snapshot,

                    currentPriority =
                        currentPriority,

                    currentAttentionRequired =
                        currentAttentionRequired,

                    currentConflict =
                        currentConflict,
                ),
            )

        val trace =
            snapshot.traceId
                ?.let {
                    tracePersistence.find(it)
                }

        return reportBuilder.build(
            replay = replay,
            originalTrace = trace,
        )
    }
}
