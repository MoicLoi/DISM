package me.moiclathy.dism.external.schedule.sync.projection

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalRepository
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

/** 从 Journal Repository 读取并生成 Run Projection 的应用边界。 */
class ExternalScheduleSyncRunProjectionService(
    private val journalRepository: ExternalScheduleSyncJournalRepository,
    private val projector: ExternalScheduleSyncRunProjector = DeterministicExternalScheduleSyncRunProjector(),
) {
    fun project(
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncRunProjectionResult {
        val snapshot = journalRepository.snapshot(seed.scheduleId, seed.runId)
        return projector.project(seed, snapshot, projectedAt)
    }

    fun project(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncRunProjectionResult {
        if (seed.scheduleId != scheduleId) return ExternalScheduleSyncRunProjectionResult.failure("seed scheduleId does not match requested scheduleId")
        if (seed.runId != runId) return ExternalScheduleSyncRunProjectionResult.failure("seed runId does not match requested runId")
        return project(seed, projectedAt)
    }
}
