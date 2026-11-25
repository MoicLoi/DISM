package me.moiclathy.dism.external.schedule.sync.journal

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Sync Journal 持久化边界。
 */
interface ExternalScheduleSyncJournalRepository {

    fun append(
        event:
            ExternalScheduleSyncJournalEvent,
    ):
        ExternalScheduleSyncJournalEvent

    fun appendAll(
        events:
            List<ExternalScheduleSyncJournalEvent>,
    ):
        List<ExternalScheduleSyncJournalEvent>

    fun findByRun(
        runId:
            ExternalScheduleSyncRunId,
    ):
        List<ExternalScheduleSyncJournalEvent>

    fun findBySchedule(
        scheduleId:
            ExternalScheduleId,
    ):
        List<ExternalScheduleSyncJournalEvent>

    fun snapshot(
        scheduleId:
            ExternalScheduleId,

        runId:
            ExternalScheduleSyncRunId,
    ):
        ExternalScheduleSyncJournalSnapshot
}
