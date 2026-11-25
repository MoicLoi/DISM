package me.moiclathy.dism.external.schedule.sync.journal.integration

import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent

interface ExternalScheduleSyncJournalObservationWriter {
    fun appendAll(events: List<ExternalScheduleSyncJournalEvent>): ExternalScheduleSyncJournalObservationWriteResult
}

data class ExternalScheduleSyncJournalObservationWriteResult(
    val successful: Boolean,
    val persistedEventCount: Int = 0,
    val warnings: List<String> = emptyList(),
    val errors: List<String> = emptyList(),
)
