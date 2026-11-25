package me.moiclathy.dism.external.schedule.sync.replay

import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRepository

/**
 * Replay + Run Repository write-back. This is state rehydration only;
 * it never retries CREATE/UPDATE/REMOVE actions.
 */
class DeterministicExternalScheduleSyncJournalRunRehydrator(
    private val replayService: ExternalScheduleSyncJournalReplayService,
    private val runRepository: ExternalScheduleSyncRunRepository,
) : ExternalScheduleSyncJournalRunRehydrator {
    override fun rehydrate(
        request: ExternalScheduleSyncJournalReplayRequest,
    ): ExternalScheduleSyncJournalReplayResult {
        if (request.mode != ExternalScheduleSyncJournalReplayMode.FULL_REPLAY) {
            return ExternalScheduleSyncJournalReplayResult(
                scheduleId = request.scheduleId,
                runId = request.runId,
                status = ExternalScheduleSyncJournalReplayStatus.FAILED,
                errors = listOf("Run rehydration requires FULL_REPLAY mode"),
            )
        }

        val result = replayService.replay(request)
        val projection = result.projection ?: return result
        if (result.status != ExternalScheduleSyncJournalReplayStatus.REPLAYED) return result

        return try {
            runRepository.save(projection.record)
            result
        } catch (exception: Exception) {
            result.copy(
                status = ExternalScheduleSyncJournalReplayStatus.FAILED,
                errors = result.errors + (exception.message ?: "Failed to persist rehydrated Run State"),
            )
        }
    }
}
