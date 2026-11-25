package me.moiclathy.dism.external.schedule.sync.replay

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

interface ExternalScheduleSyncJournalReplayValidator {
    fun validate(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        events: List<ExternalScheduleSyncJournalEvent>,
    ): List<String>
}
