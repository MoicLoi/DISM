package me.moiclathy.dism.external.schedule.sync.replay

interface ExternalScheduleSyncJournalReplayService {
    fun replay(request: ExternalScheduleSyncJournalReplayRequest): ExternalScheduleSyncJournalReplayResult
}
