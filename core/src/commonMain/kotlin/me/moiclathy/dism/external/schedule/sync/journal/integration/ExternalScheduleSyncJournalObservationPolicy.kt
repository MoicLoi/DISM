package me.moiclathy.dism.external.schedule.sync.journal.integration

/**
 * Controls which 11.32 facts are emitted into the Journal.
 *
 * The defaults intentionally preserve the full execution audit trail.
 */
data class ExternalScheduleSyncJournalObservationPolicy(
    val includeRunMarkers: Boolean = true,
    val includeRecoveryMarkers: Boolean = true,
    val includeNoOpObservation: Boolean = true,
    val includeCompleteSyncObservation: Boolean = true,
    val includeActionStarted: Boolean = true,
    val includeActionTerminal: Boolean = true,
    val includeEventMilestones: Boolean = true,
)
