package me.moiclathy.dism.external.schedule.sync

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.diff.DeterministicExternalScheduleDiffer
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffRecord
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiffType
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord

/**
 * 确定性的 External Schedule Sync Plan Executor。
 *
 * 注意：
 * 这里生成的是执行计划。
 *
 * 不直接修改：
 *
 * - Event
 * - Entity
 * - State
 * - Attention
 * - Persistence
 */
class DeterministicExternalScheduleSyncExecutor(
    private val differ:
        DeterministicExternalScheduleDiffer =
        DeterministicExternalScheduleDiffer(),
) : ExternalScheduleSyncExecutor {

    override fun plan(
        scheduleId: ExternalScheduleId,

        previous:
            List<ExternalSchedulePersistenceRecord>,

        current:
            List<ExternalSchedulePersistenceRecord>,

        policy:
            ExternalScheduleSyncPolicy,
    ): ExternalScheduleSyncResult {

        val diffResult =
            differ.diff(
                scheduleId = scheduleId,

                previous = previous,

                current = current,

                policy =
                    policy.diffPolicy,
            )

        if (!diffResult.successful) {

            return ExternalScheduleSyncResult.failure(
                errors =
                    diffResult.errors,

                warnings =
                    diffResult.warnings,
            )
        }

        val diff =
            diffResult.diff
                ?: return ExternalScheduleSyncResult.failure(
                    errors =
                        listOf(
                            "Diff result did not contain a diff"
                        )
                )

        val changes =
            mutableListOf<ExternalScheduleSyncChange>()

        for (record in diff.records) {

            when (record.type) {

                ExternalScheduleDiffType.ADDED -> {

                    if (policy.allowCreate) {

                        changes +=
                            ExternalScheduleSyncChange(
                                externalKey =
                                    record.externalKey,

                                action =
                                    ExternalScheduleSyncAction
                                        .CREATE_EVENT,

                                diff =
                                    record,
                            )
                    }
                }

                ExternalScheduleDiffType.CHANGED -> {

                    if (policy.allowUpdate) {

                        changes +=
                            ExternalScheduleSyncChange(
                                externalKey =
                                    record.externalKey,

                                action =
                                    ExternalScheduleSyncAction
                                        .UPDATE_EVENT,

                                diff =
                                    record,
                            )
                    }
                }

                ExternalScheduleDiffType.UNCHANGED -> {

                    if (policy.includeNoOp) {

                        changes +=
                            ExternalScheduleSyncChange(
                                externalKey =
                                    record.externalKey,

                                action =
                                    ExternalScheduleSyncAction
                                        .NO_OP,

                                diff =
                                    record,
                            )
                    }
                }

                ExternalScheduleDiffType.REMOVED -> {

                    if (
                        policy.allowRemovalHandling
                    ) {

                        changes +=
                            ExternalScheduleSyncChange(
                                externalKey =
                                    record.externalKey,

                                action =
                                    ExternalScheduleSyncAction
                                        .HANDLE_REMOVED_EVENT,

                                diff =
                                    record,
                            )
                    }
                }
            }
        }

        val warnings =
            mutableListOf<String>()

        if (
            !policy.allowCreate &&
            diff.addedCount > 0
        ) {
            warnings +=
                "${diff.addedCount} added records were not planned because creation is disabled"
        }

        if (
            !policy.allowUpdate &&
            diff.changedCount > 0
        ) {
            warnings +=
                "${diff.changedCount} changed records were not planned because update is disabled"
        }

        if (
            !policy.allowRemovalHandling &&
            diff.removedCount > 0
        ) {
            warnings +=
                "${diff.removedCount} removed records were not planned because removal handling is disabled"
        }

        val plan =
            ExternalScheduleSyncPlan(
                scheduleId =
                    scheduleId,

                diff =
                    diff,

                changes =
                    changes,
            )

        return ExternalScheduleSyncResult.success(
            plan =
                plan,

            warnings =
                diffResult.warnings +
                    warnings,
        )
    }
}
