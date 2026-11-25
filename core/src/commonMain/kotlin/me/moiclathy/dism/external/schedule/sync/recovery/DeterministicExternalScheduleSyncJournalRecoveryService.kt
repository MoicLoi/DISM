package me.moiclathy.dism.external.schedule.sync.recovery

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceRepository
import me.moiclathy.dism.external.schedule.sync.projection.DeterministicExternalScheduleSyncRunProjector
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

class DeterministicExternalScheduleSyncJournalRecoveryService(
    persistenceRepository: ExternalScheduleSyncJournalPersistenceRepository,
    projector: DeterministicExternalScheduleSyncRunProjector,
) : ExternalScheduleSyncJournalRecoveryService {

    private val bootstrap = ExternalScheduleSyncJournalRecoveryBootstrap(
        persistenceRepository = persistenceRepository,
        projector = projector,
    )

    override fun recover(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        seed: ExternalScheduleSyncRunRecord,
        projectedAt: Instant,
    ): ExternalScheduleSyncJournalRecoveryResult =
        bootstrap.recover(scheduleId, runId, seed, projectedAt)
}
