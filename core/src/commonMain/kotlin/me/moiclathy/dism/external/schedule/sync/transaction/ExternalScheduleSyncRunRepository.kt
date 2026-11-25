package me.moiclathy.dism.external.schedule.sync.transaction

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * Sync Run 持久化边界。
 */
interface ExternalScheduleSyncRunRepository {

    fun find(
        runId:
            ExternalScheduleSyncRunId,
    ):
        ExternalScheduleSyncRunRecord?

    fun findLatest(
        scheduleId:
            ExternalScheduleId,
    ):
        ExternalScheduleSyncRunRecord?

    fun save(
        record:
            ExternalScheduleSyncRunRecord,
    ):
        ExternalScheduleSyncRunRecord

    fun delete(
        runId:
            ExternalScheduleSyncRunId,
    )
}
