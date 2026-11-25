package me.moiclathy.dism.external.schedule.sync.journal.integration

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.sync.e2e.DeterministicExternalScheduleSyncE2ERunner
import me.moiclathy.dism.external.schedule.sync.e2e.ExternalScheduleSyncE2EInput
import me.moiclathy.dism.external.schedule.sync.e2e.ExternalScheduleSyncE2EResult

/**
 * 11.32 integration boundary:
 *
 * 11.31 E2E
 *   -> observation sink
 *   -> 11.22 Journal facts
 *   -> 11.24 persistence service
 *
 * The runner still does not know SQLDelight or concrete Event internals.
 */
class ExternalScheduleSyncJournalIntegratedE2ERunner(
    private val e2eRunner: DeterministicExternalScheduleSyncE2ERunner,
    private val mapper: ExternalScheduleSyncJournalObservationMapper =
        DeterministicExternalScheduleSyncJournalObservationMapper(),
    private val writer: ExternalScheduleSyncJournalObservationWriter,
    private val policy: ExternalScheduleSyncJournalObservationPolicy =
        ExternalScheduleSyncJournalObservationPolicy(),
) {

    fun run(
        input: ExternalScheduleSyncE2EInput,
    ): ExternalScheduleSyncJournalObservationResult {
        val sequenceBase = 0
        val preEvents = mutableListOf<me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent>()

        if (policy.includeRunMarkers) {
            preEvents += mapper.mapRunStarted(
                input.scheduleId,
                input.runId,
                input.projectedAt,
                sequenceBase,
            )
        }
        if (policy.includeRecoveryMarkers) {
            preEvents += mapper.mapRecoveryStarted(
                input.scheduleId,
                input.runId,
                input.projectedAt,
                sequenceBase + preEvents.size,
            )
        }

        val preWrite = writer.appendAll(preEvents)
        var persisted = preWrite.persistedEventCount
        val warnings = preWrite.warnings.toMutableList()
        val errors = preWrite.errors.toMutableList()
        if (!preWrite.successful) {
            return ExternalScheduleSyncJournalObservationResult(
                runId = input.runId,
                status = ExternalScheduleSyncJournalObservationStatus.FAILED,
                attemptedEventCount = preEvents.size,
                persistedEventCount = persisted,
                events = preEvents,
                warnings = warnings,
                errors = errors,
            )
        }

        val sink = ExternalScheduleSyncJournalObservationSink(
            scheduleId = input.scheduleId,
            runId = input.runId,
            occurredAt = input.projectedAt,
            mapper = mapper,
            writer = writer,
            nextSequence = preEvents.size,
        )

        val e2eResult = runCatching {
            e2eRunner.run(input.copy(observationSink = sink))
        }.getOrElse { throwable ->
            warnings += "11.31 execution failed while journal observation was active"
            errors += throwable.message ?: throwable::class.simpleName ?: "11.31 execution failed"
            null
        }

        persisted += sink.persistedEventCount
        warnings += sink.warnings
        errors += sink.errors

        val postEvents = mutableListOf<me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent>()
        val postSequence = preEvents.size + sink.attemptedEventCount

        if (policy.includeRecoveryMarkers) {
            postEvents += mapper.mapRecoveryCompleted(
                input.scheduleId,
                input.runId,
                input.projectedAt,
                postSequence,
                e2eResult?.trace?.reconciliation?.status?.name,
            )
        }

        if (policy.includeRunMarkers) {
            postEvents += mapper.mapRunCompleted(
                input.scheduleId,
                input.runId,
                input.projectedAt,
                postSequence + postEvents.size,
                e2eResult?.status?.name ?: "FAILED",
            )
        }

        val postWrite = writer.appendAll(postEvents)
        persisted += postWrite.persistedEventCount
        warnings += postWrite.warnings
        errors += postWrite.errors

        val attempted = preEvents.size + sink.attemptedEventCount + postEvents.size
        val events = preEvents + sink.observedEvents + postEvents
        val status = when {
            errors.isNotEmpty() -> ExternalScheduleSyncJournalObservationStatus.PARTIAL
            attempted == 0 -> ExternalScheduleSyncJournalObservationStatus.NO_EVENTS
            else -> ExternalScheduleSyncJournalObservationStatus.COMPLETED
        }

        return ExternalScheduleSyncJournalObservationResult(
            runId = input.runId,
            status = status,
            attemptedEventCount = attempted,
            persistedEventCount = persisted,
            events = events,
            warnings = warnings,
            errors = errors,
        )
    }
}
