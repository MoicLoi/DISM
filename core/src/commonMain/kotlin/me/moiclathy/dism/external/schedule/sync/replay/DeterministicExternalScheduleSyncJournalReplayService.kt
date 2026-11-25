package me.moiclathy.dism.external.schedule.sync.replay

import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalSnapshot
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncJournalPersistenceService
import me.moiclathy.dism.external.schedule.sync.projection.DeterministicExternalScheduleSyncRunProjector
import me.moiclathy.dism.external.schedule.sync.projection.ExternalScheduleSyncRunProjector

/**
 * 11.33: durable Journal -> Snapshot -> Checkpoint -> optional Run Projection.
 * It intentionally does not execute sync actions or touch Event internals.
 */
class DeterministicExternalScheduleSyncJournalReplayService(
    private val journalPersistence: ExternalScheduleSyncJournalPersistenceService,
    private val projector: ExternalScheduleSyncRunProjector = DeterministicExternalScheduleSyncRunProjector(),
    private val validator: ExternalScheduleSyncJournalReplayValidator =
        DeterministicExternalScheduleSyncJournalReplayValidator(),
) : ExternalScheduleSyncJournalReplayService {

    override fun replay(request: ExternalScheduleSyncJournalReplayRequest): ExternalScheduleSyncJournalReplayResult {
        val events = try {
            journalPersistence.restoreSnapshot(request.scheduleId, request.runId)
        } catch (exception: Exception) {
            return ExternalScheduleSyncJournalReplayResult(
                scheduleId = request.scheduleId,
                runId = request.runId,
                status = ExternalScheduleSyncJournalReplayStatus.FAILED,
                errors = listOf(exception.message ?: "Failed to restore Journal snapshot"),
            )
        }

        if (events.isEmpty()) {
            return ExternalScheduleSyncJournalReplayResult(
                scheduleId = request.scheduleId,
                runId = request.runId,
                status = ExternalScheduleSyncJournalReplayStatus.EMPTY,
                replayedEventCount = 0,
            )
        }

        val validationErrors = validator.validate(request.scheduleId, request.runId, events)
        if (validationErrors.isNotEmpty()) {
            return ExternalScheduleSyncJournalReplayResult(
                scheduleId = request.scheduleId,
                runId = request.runId,
                status = ExternalScheduleSyncJournalReplayStatus.INVALID,
                replayedEventCount = events.size,
                errors = validationErrors,
            )
        }

        val snapshot = ExternalScheduleSyncJournalSnapshot(
            scheduleId = request.scheduleId,
            runId = request.runId,
            events = events,
        )
        val checkpoints = snapshot.checkpoints.map {
            ExternalScheduleSyncJournalReplayCheckpoint(
                externalKey = it.externalKey,
                action = it.action,
                status = it.status,
                eventId = it.eventId,
                startedAt = it.startedAt,
                lastUpdatedAt = it.lastUpdatedAt,
                message = it.message,
            )
        }

        if (request.mode == ExternalScheduleSyncJournalReplayMode.VALIDATE_ONLY) {
            return ExternalScheduleSyncJournalReplayResult(
                scheduleId = request.scheduleId,
                runId = request.runId,
                status = ExternalScheduleSyncJournalReplayStatus.REPLAYED,
                snapshot = snapshot,
                checkpoints = checkpoints,
                replayedEventCount = events.size,
            )
        }

        if (request.mode == ExternalScheduleSyncJournalReplayMode.CHECKPOINT_REHYDRATION) {
            return ExternalScheduleSyncJournalReplayResult(
                scheduleId = request.scheduleId,
                runId = request.runId,
                status = if (snapshot.pendingCount > 0 || snapshot.failedCount > 0)
                    ExternalScheduleSyncJournalReplayStatus.PARTIAL
                else ExternalScheduleSyncJournalReplayStatus.REPLAYED,
                snapshot = snapshot,
                checkpoints = checkpoints,
                replayedEventCount = events.size,
                warnings = buildList {
                    if (snapshot.pendingCount > 0) add("Journal replay contains ${snapshot.pendingCount} pending checkpoint(s)")
                    if (snapshot.failedCount > 0) add("Journal replay contains ${snapshot.failedCount} failed checkpoint(s)")
                },
            )
        }

        val projectionResult = projector.project(
            seed = request.seed,
            snapshot = snapshot,
            projectedAt = request.projectedAt,
        )
        if (!projectionResult.successful || projectionResult.projection == null) {
            return ExternalScheduleSyncJournalReplayResult(
                scheduleId = request.scheduleId,
                runId = request.runId,
                status = ExternalScheduleSyncJournalReplayStatus.PARTIAL,
                snapshot = snapshot,
                checkpoints = checkpoints,
                replayedEventCount = events.size,
                warnings = projectionResult.warnings,
                errors = projectionResult.errors,
            )
        }

        return ExternalScheduleSyncJournalReplayResult(
            scheduleId = request.scheduleId,
            runId = request.runId,
            status = ExternalScheduleSyncJournalReplayStatus.REPLAYED,
            snapshot = snapshot,
            projection = projectionResult.projection,
            checkpoints = checkpoints,
            replayedEventCount = events.size,
            warnings = projectionResult.warnings,
            errors = projectionResult.errors,
        )
    }
}
