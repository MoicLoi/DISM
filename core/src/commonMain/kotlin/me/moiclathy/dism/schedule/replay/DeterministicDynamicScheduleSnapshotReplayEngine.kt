package me.moiclathy.dism.schedule.replay

enum class DynamicScheduleSnapshotReplayDifferenceType {

    PRIORITY_CHANGED,

    ATTENTION_DECISION_CHANGED,

    CONFLICT_CHANGED,
}

data class DynamicScheduleSnapshotReplayDifference(
    val type:
        DynamicScheduleSnapshotReplayDifferenceType,

    val originalValue: String,

    val currentValue: String,

    val message: String,
)

class DeterministicDynamicScheduleSnapshotReplayEngine {

    fun replay(
        input:
            DynamicScheduleSnapshotReplayInput,
    ): DynamicScheduleSnapshotReplayResult {

        val snapshot =
            input.snapshot

        val originalPriority =
            calculateOriginalPriority(snapshot)

        val originalAttentionRequired =
            calculateOriginalAttentionRequired(snapshot)

        val currentPriority =
            input.currentPriority

        val currentAttentionRequired =
            input.currentAttentionRequired

        val differences =
            mutableListOf<
                DynamicScheduleSnapshotReplayDifference
            >()

        if (
            originalPriority !=
                currentPriority
        ) {
            differences +=
                DynamicScheduleSnapshotReplayDifference(
                    type =
                        DynamicScheduleSnapshotReplayDifferenceType
                            .PRIORITY_CHANGED,

                    originalValue =
                        originalPriority.toString(),

                    currentValue =
                        currentPriority.toString(),

                    message =
                        "Decision priority changed",
                )
        }

        if (
            originalAttentionRequired !=
                currentAttentionRequired
        ) {
            differences +=
                DynamicScheduleSnapshotReplayDifference(
                    type =
                        DynamicScheduleSnapshotReplayDifferenceType
                            .ATTENTION_DECISION_CHANGED,

                    originalValue =
                        originalAttentionRequired.toString(),

                    currentValue =
                        currentAttentionRequired.toString(),

                    message =
                        "Attention decision changed",
                )
        }

        val originalConflict =
            !snapshot.dependency.requiredSatisfied

        val currentConflict =
            input.currentConflict
                ?.conflict != null

        if (
            originalConflict !=
                currentConflict
        ) {
            differences +=
                DynamicScheduleSnapshotReplayDifference(
                    type =
                        DynamicScheduleSnapshotReplayDifferenceType
                            .CONFLICT_CHANGED,

                    originalValue =
                        originalConflict.toString(),

                    currentValue =
                        currentConflict.toString(),

                    message =
                        "Decision conflict state changed",
                )
        }

        return DynamicScheduleSnapshotReplayResult(
            snapshot =
                snapshot,

            originalPriority =
                originalPriority,

            currentPriority =
                currentPriority,

            originalAttentionRequired =
                originalAttentionRequired,

            currentAttentionRequired =
                currentAttentionRequired,

            priorityChanged =
                originalPriority !=
                    currentPriority,

            attentionDecisionChanged =
                originalAttentionRequired !=
                    currentAttentionRequired,

            conflictChanged =
                originalConflict !=
                    currentConflict,

            differences =
                differences,
        )
    }

    private fun calculateOriginalPriority(
        snapshot:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshot,
    ): Double {

        val base =
            snapshot.item.priority

        val memory =
            snapshot.attentionMemory.effectiveFactor

        /*
         * Memory 只决定软优先级。
         */
        val softPriority =
            (
                base * memory
                ).coerceIn(0.0, 1.0)

        /*
         * 12.18 修正：
         *
         * Dependency 是 hard constraint，
         * 未满足的必需依赖必须把优先级抬到
         * minimumHardConstraintPriority 之上，
         * 否则 12.12 的 hard constraint 语义会在 replay 中丢失。
         */
        if (
            !snapshot.dependency.requiredSatisfied
        ) {
            return maxOf(
                softPriority,
                snapshot.policy
                    .minimumHardConstraintPriority,
            )
        }

        return softPriority
    }

    private fun calculateOriginalAttentionRequired(
        snapshot:
            me.moiclathy.dism.schedule.snapshot
                .DynamicScheduleDecisionSnapshot,
    ): Boolean {

        if (
            !snapshot.dependency.requiredSatisfied
        ) {
            return true
        }

        val priority =
            calculateOriginalPriority(snapshot)

        return priority >=
            snapshot.policy.minimumAttentionPriority
    }
}
