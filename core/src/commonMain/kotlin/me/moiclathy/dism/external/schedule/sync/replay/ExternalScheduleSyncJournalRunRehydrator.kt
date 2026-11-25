package me.moiclathy.dism.external.schedule.sync.replay

import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

interface ExternalScheduleSyncJournalRunRehydrator {
    fun rehydrate(request: ExternalScheduleSyncJournalReplayRequest): ExternalScheduleSyncJournalReplayResult
}
