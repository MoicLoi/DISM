package me.moiclathy.dism.schedule.replay

import me.moiclathy.dism.schedule.snapshot.DynamicScheduleDecisionSnapshot
import me.moiclathy.dism.schedule.runtime.DynamicScheduleIntelligenceResult

/**
 * 12.27
 *
 * Historical Runtime Replay 与原始 Decision Snapshot
 * 之间的差异。
 */
data class DynamicScheduleHistoricalRuntimeReplayDifference(
    val itemId:
        String,

    val originalPriority:
        Double,

    val replayedPriority:
        Double,

    val priorityChanged:
        Boolean,

    val originalAttentionRequired:
        Boolean,

    val replayedAttentionRequired:
        Boolean,

    val attentionDecisionChanged:
        Boolean,

    val originalConflict:
        Boolean,

    val replayedConflict:
        Boolean,

    val conflictChanged:
        Boolean,

    val originalExplanationExists:
        Boolean,

    val replayedExplanationExists:
        Boolean,

    val explanationChanged:
        Boolean,
) {

    val changed:
        Boolean
        get() =
            priorityChanged ||
                attentionDecisionChanged ||
                conflictChanged ||
                explanationChanged

    companion object {

        fun compare(
            snapshot:
                DynamicScheduleDecisionSnapshot,

            replayed:
                DynamicScheduleIntelligenceResult,
        ):
            DynamicScheduleHistoricalRuntimeReplayDifference {

            /*
             * 12.27 修正：
             *
             * ProjectionResult 暴露的是 candidates，
             * 每个 candidate 通过 candidate.recommendation 携带 priority。
             */
            val replayedCandidate =
                replayed
                    .attentionProjection
                    .candidates
                    .firstOrNull {
                        it.itemId == snapshot.item.itemId
                    }

            val replayedPriority =
                replayedCandidate
                    ?.candidate
                    ?.recommendation
                    ?.priority
                    ?: snapshot.item.priority

            val replayedAttentionRequired =
                replayed
                    .attentionConflicts[
                        snapshot.item.itemId
                    ]
                    ?.attentionRequired
                    ?: false

            val originalConflict =
                !snapshot
                    .dependency
                    .requiredSatisfied

            val replayedConflict =
                replayed
                    .attentionConflicts[
                        snapshot.item.itemId
                    ]
                    ?.conflict != null

            val originalExplanationExists =
                snapshot
                    .traceId != null

            val replayedExplanationExists =
                replayed
                    .attentionExplanations
                    .containsKey(
                        snapshot.item.itemId,
                    )

            return DynamicScheduleHistoricalRuntimeReplayDifference(
                itemId =
                    snapshot
                        .item
                        .itemId
                        .toString(),

                originalPriority =
                    snapshot
                        .item
                        .priority,

                replayedPriority =
                    replayedPriority,

                priorityChanged =
                    snapshot
                        .item
                        .priority !=
                        replayedPriority,

                originalAttentionRequired =
                    originalConflict ||
                        snapshot
                            .item
                            .priority >=
                        snapshot
                            .policy
                            .minimumAttentionPriority,

                replayedAttentionRequired =
                    replayedAttentionRequired,

                attentionDecisionChanged =
                    (
                        originalConflict ||
                            snapshot
                                .item
                                .priority >=
                            snapshot
                                .policy
                                .minimumAttentionPriority
                        ) !=
                        replayedAttentionRequired,

                originalConflict =
                    originalConflict,

                replayedConflict =
                    replayedConflict,

                conflictChanged =
                    originalConflict !=
                        replayedConflict,

                originalExplanationExists =
                    originalExplanationExists,

                replayedExplanationExists =
                    replayedExplanationExists,

                explanationChanged =
                    originalExplanationExists !=
                        replayedExplanationExists,
            )
        }
    }
}
