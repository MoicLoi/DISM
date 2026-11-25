package me.moiclathy.dism.external.schedule.sync

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord

/**
 * External Schedule Sync Executor。
 *
 * 负责：
 *
 * previous persistence
 *        +
 * current persistence
 *        ↓
 *      Diff
 *        ↓
 *      Sync Plan
 *
 * 本接口不要求直接操作 Event。
 */
interface ExternalScheduleSyncExecutor {

    fun plan(
        scheduleId: ExternalScheduleId,

        previous:
            List<ExternalSchedulePersistenceRecord>,

        current:
            List<ExternalSchedulePersistenceRecord>,

        policy:
            ExternalScheduleSyncPolicy =
            ExternalScheduleSyncPolicy(),
    ): ExternalScheduleSyncResult
}
