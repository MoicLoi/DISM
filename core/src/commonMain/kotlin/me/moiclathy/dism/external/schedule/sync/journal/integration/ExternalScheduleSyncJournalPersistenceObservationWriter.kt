package me.moiclathy.dism.external.schedule.sync.journal.integration

import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceService

/**
 * 11.32 -> 11.24 bridge. No SQLDelight API is guessed here; the existing
 * persistence service remains the only storage boundary.
 */
class ExternalScheduleSyncJournalPersistenceObservationWriter(
    private val persistenceService: ExternalScheduleSyncJournalPersistenceService,
) : ExternalScheduleSyncJournalObservationWriter {
    override fun appendAll(
        events: List<ExternalScheduleSyncJournalEvent>,
    ): ExternalScheduleSyncJournalObservationWriteResult {
        if (events.isEmpty()) {
            return ExternalScheduleSyncJournalObservationWriteResult(
                successful = true,
                persistedEventCount = 0,
            )
        }

        val result = persistenceService.appendAll(events)
        return ExternalScheduleSyncJournalObservationWriteResult(
            successful = result.successful,
            persistedEventCount = if (result.successful) result.affectedRecords else 0,
            warnings = result.warnings,
            errors = result.errors,
        )
    }
}
