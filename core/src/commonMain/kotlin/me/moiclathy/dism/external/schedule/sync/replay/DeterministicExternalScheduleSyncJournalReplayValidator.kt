package me.moiclathy.dism.external.schedule.sync.replay

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

class DeterministicExternalScheduleSyncJournalReplayValidator : ExternalScheduleSyncJournalReplayValidator {
    override fun validate(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        events: List<ExternalScheduleSyncJournalEvent>,
    ): List<String> {
        val errors = mutableListOf<String>()
        val ids = mutableSetOf<String>()

        events.forEach { event ->
            if (event.scheduleId != scheduleId) {
                errors += "Journal event ${event.journalId.value} has a different scheduleId"
            }
            if (event.runId != runId) {
                errors += "Journal event ${event.journalId.value} has a different runId"
            }
            if (!ids.add(event.journalId.value)) {
                errors += "Duplicate journalId: ${event.journalId.value}"
            }
        }

        val hasTerminal = events.any {
            it.type == ExternalScheduleSyncJournalEventType.ACTION_COMPLETED ||
                it.type == ExternalScheduleSyncJournalEventType.ACTION_FAILED ||
                it.type == ExternalScheduleSyncJournalEventType.ACTION_SKIPPED
        }
        if (hasTerminal && events.none { it.type == ExternalScheduleSyncJournalEventType.ACTION_STARTED }) {
            errors += "Terminal action Journal event exists without any ACTION_STARTED event"
        }

        return errors.distinct().sorted()
    }
}
