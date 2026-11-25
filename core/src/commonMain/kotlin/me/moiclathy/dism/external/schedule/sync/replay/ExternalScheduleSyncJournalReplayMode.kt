package me.moiclathy.dism.external.schedule.sync.replay

enum class ExternalScheduleSyncJournalReplayMode {
    FULL_REPLAY,
    CHECKPOINT_REHYDRATION,
    VALIDATE_ONLY,
}
