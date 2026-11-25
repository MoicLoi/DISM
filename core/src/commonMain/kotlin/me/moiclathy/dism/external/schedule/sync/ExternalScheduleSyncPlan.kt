package me.moiclathy.dism.external.schedule.sync

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.diff.ExternalScheduleDiff

/**
 * 一次 Schedule Sync 的执行计划。
 *
 * 注意：
 * Plan 本身不修改任何状态。
 */
data class ExternalScheduleSyncPlan(
    val scheduleId: ExternalScheduleId,

    val diff: ExternalScheduleDiff,

    val changes: List<ExternalScheduleSyncChange>,
) {

    val creates:
        List<ExternalScheduleSyncChange>
        get() =
            changes.filter {
                it.action ==
                    ExternalScheduleSyncAction.CREATE_EVENT
            }

    val updates:
        List<ExternalScheduleSyncChange>
        get() =
            changes.filter {
                it.action ==
                    ExternalScheduleSyncAction.UPDATE_EVENT
            }

    val removals:
        List<ExternalScheduleSyncChange>
        get() =
            changes.filter {
                it.action ==
                    ExternalScheduleSyncAction
                        .HANDLE_REMOVED_EVENT
            }

    val noOps:
        List<ExternalScheduleSyncChange>
        get() =
            changes.filter {
                it.action ==
                    ExternalScheduleSyncAction.NO_OP
            }

    val createCount: Int
        get() = creates.size

    val updateCount: Int
        get() = updates.size

    val removalCount: Int
        get() = removals.size

    val noOpCount: Int
        get() = noOps.size
}
