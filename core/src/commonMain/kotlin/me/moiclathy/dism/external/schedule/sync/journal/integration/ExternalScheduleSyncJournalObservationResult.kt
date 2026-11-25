package me.moiclathy.dism.external.schedule.sync.journal.integration

import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

data class ExternalScheduleSyncJournalObservationResult(
    val runId: ExternalScheduleSyncRunId,
    val status: ExternalScheduleSyncJournalObservationStatus,
    val attemptedEventCount: Int,
    val persistedEventCount: Int,
    val events: List<ExternalScheduleSyncJournalEvent> = emptyList(),
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
) {
    init {
        require(attemptedEventCount >= 0)
        require(persistedEventCount >= 0)
        require(persistedEventCount <= attemptedEventCount)
    }

    val successful: Boolean
        get() = status == ExternalScheduleSyncJournalObservationStatus.COMPLETED ||
            status == ExternalScheduleSyncJournalObservationStatus.NO_EVENTS
}
