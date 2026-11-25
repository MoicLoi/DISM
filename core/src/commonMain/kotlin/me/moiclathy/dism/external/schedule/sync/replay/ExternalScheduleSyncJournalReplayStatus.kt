package me.moiclathy.dism.external.schedule.sync.replay

enum class ExternalScheduleSyncJournalReplayStatus {
    REPLAYED,
    EMPTY,
    PARTIAL,
    INVALID,
    FAILED,
}
