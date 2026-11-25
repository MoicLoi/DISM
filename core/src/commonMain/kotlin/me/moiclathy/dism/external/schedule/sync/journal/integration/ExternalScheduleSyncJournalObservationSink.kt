package me.moiclathy.dism.external.schedule.sync.journal.integration

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.e2e.ExternalScheduleSyncE2EActionTrace
import me.moiclathy.dism.external.schedule.sync.e2e.ExternalScheduleSyncE2EObservationSink
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Streaming ObservationSink for 11.31 action traces.
 *
 * Run/recovery markers are emitted by the integrated runner; this sink only
 * handles per-action observations.
 */
class ExternalScheduleSyncJournalObservationSink(
    private val scheduleId: ExternalScheduleId,
    private val runId: ExternalScheduleSyncRunId,
    private val occurredAt: Instant,
    private val mapper: ExternalScheduleSyncJournalObservationMapper,
    private val writer: ExternalScheduleSyncJournalObservationWriter,
    private var nextSequence: Int,
) : ExternalScheduleSyncE2EObservationSink {

    var persistedEventCount: Int = 0
        private set

    var attemptedEventCount: Int = 0
        private set

    val observedEvents: MutableList<me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent> =
        mutableListOf()

    val warnings: MutableList<String> = mutableListOf()
    val errors: MutableList<String> = mutableListOf()

    override fun observe(trace: ExternalScheduleSyncE2EActionTrace) {
        val events = mapper.mapAction(
            scheduleId = scheduleId,
            runId = runId,
            trace = trace,
            occurredAt = occurredAt,
            sequenceStart = nextSequence,
        )
        nextSequence += events.size
        attemptedEventCount += events.size
        observedEvents += events
        val write = writer.appendAll(events)
        persistedEventCount += write.persistedEventCount
        warnings += write.warnings
        errors += write.errors
        if (!write.successful) {
            throw IllegalStateException(
                write.errors.firstOrNull() ?: "Failed to persist journal observation",
            )
        }
    }
}
