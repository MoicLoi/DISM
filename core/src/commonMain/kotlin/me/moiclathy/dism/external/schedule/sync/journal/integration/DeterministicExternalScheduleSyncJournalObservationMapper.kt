package me.moiclathy.dism.external.schedule.sync.journal.integration

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.e2e.ExternalScheduleSyncE2EActionTrace
import me.moiclathy.dism.external.schedule.sync.execution.ExternalScheduleSyncActionExecutionStatus
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEvent
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalEventType
import me.moiclathy.dism.external.schedule.sync.journal.ExternalScheduleSyncJournalId
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId

/**
 * Deterministic mapping from 11.31 execution traces to 11.22 Journal facts.
 *
 * Journal IDs include run + sequence so replaying the same observation is
 * idempotent at the 11.24 persistence layer's journal-id boundary.
 */
class DeterministicExternalScheduleSyncJournalObservationMapper(
    private val policy: ExternalScheduleSyncJournalObservationPolicy =
        ExternalScheduleSyncJournalObservationPolicy(),
) : ExternalScheduleSyncJournalObservationMapper {

    override fun mapRunStarted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
    ): ExternalScheduleSyncJournalEvent = event(
        scheduleId, runId, sequence, ExternalScheduleSyncJournalEventType.RUN_STARTED,
        occurredAt = occurredAt,
    )

    override fun mapRecoveryStarted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
    ): ExternalScheduleSyncJournalEvent = event(
        scheduleId, runId, sequence, ExternalScheduleSyncJournalEventType.RECOVERY_STARTED,
        occurredAt = occurredAt,
    )

    override fun mapAction(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        trace: ExternalScheduleSyncE2EActionTrace,
        occurredAt: Instant,
        sequenceStart: Int,
    ): List<ExternalScheduleSyncJournalEvent> {
        val events = mutableListOf<ExternalScheduleSyncJournalEvent>()
        var sequence = sequenceStart

        fun add(
            type: ExternalScheduleSyncJournalEventType,
            message: String? = null,
            eventId: String? = null,
        ) {
            events += event(
                scheduleId = scheduleId,
                runId = runId,
                sequence = sequence++,
                type = type,
                occurredAt = occurredAt,
                externalKey = trace.externalKey,
                action = trace.syncAction,
                eventId = eventId,
                message = message,
            )
        }

        if (trace.syncAction == null) {
            val shouldObserve = when (trace.reconciliationAction) {
                ExternalScheduleSyncReconciliationAction.NO_OP -> policy.includeNoOpObservation
                ExternalScheduleSyncReconciliationAction.COMPLETE_SYNC -> policy.includeCompleteSyncObservation
                else -> true
            }
            if (shouldObserve) {
                add(
                    type = ExternalScheduleSyncJournalEventType.RECONCILIATION_OBSERVED,
                    message = trace.message ?: trace.reconciliationAction.name,
                    eventId = trace.eventId,
                )
            }
            return events
        }

        if (policy.includeActionStarted) {
            add(ExternalScheduleSyncJournalEventType.ACTION_STARTED)
        }

        when (trace.executionStatus) {
            ExternalScheduleSyncActionExecutionStatus.EXECUTED -> {
                if (policy.includeEventMilestones) {
                    when (trace.syncAction) {
                        ExternalScheduleSyncAction.CREATE_EVENT ->
                            trace.eventId?.let {
                                add(ExternalScheduleSyncJournalEventType.EVENT_CREATED, eventId = it)
                            }
                        ExternalScheduleSyncAction.UPDATE_EVENT ->
                            trace.eventId?.let {
                                add(ExternalScheduleSyncJournalEventType.EVENT_UPDATED, eventId = it)
                            }
                        ExternalScheduleSyncAction.HANDLE_REMOVED_EVENT ->
                            add(
                                ExternalScheduleSyncJournalEventType.EVENT_REMOVAL_HANDLED,
                                eventId = trace.eventId,
                            )
                        ExternalScheduleSyncAction.RECREATE_EVENT_MAPPING ->
                            trace.eventId?.let {
                                add(ExternalScheduleSyncJournalEventType.MAPPING_COMMITTED, eventId = it)
                            }
                        ExternalScheduleSyncAction.NO_OP -> Unit
                    }
                }
                if (policy.includeActionTerminal) {
                    add(
                        ExternalScheduleSyncJournalEventType.ACTION_COMPLETED,
                        message = trace.message,
                        eventId = trace.eventId,
                    )
                }
            }

            ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_IN_PROGRESS,
            ExternalScheduleSyncActionExecutionStatus.SKIPPED_ALREADY_COMPLETED -> {
                if (policy.includeActionTerminal) {
                    add(
                        ExternalScheduleSyncJournalEventType.ACTION_SKIPPED,
                        message = trace.message,
                        eventId = trace.eventId,
                    )
                }
            }

            ExternalScheduleSyncActionExecutionStatus.FAILED,
            ExternalScheduleSyncActionExecutionStatus.REJECTED,
            null -> {
                if (policy.includeActionTerminal) {
                    add(
                        ExternalScheduleSyncJournalEventType.ACTION_FAILED,
                        message = trace.errors.firstOrNull()
                            ?: trace.message
                            ?: "Action failed",
                        eventId = trace.eventId,
                    )
                }
            }
        }

        return events
    }

    override fun mapRecoveryCompleted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
        message: String?,
    ): ExternalScheduleSyncJournalEvent = event(
        scheduleId, runId, sequence, ExternalScheduleSyncJournalEventType.RECOVERY_COMPLETED,
        occurredAt = occurredAt,
        message = message,
    )

    override fun mapRunCompleted(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        occurredAt: Instant,
        sequence: Int,
        message: String?,
    ): ExternalScheduleSyncJournalEvent = event(
        scheduleId, runId, sequence, ExternalScheduleSyncJournalEventType.RUN_COMPLETED,
        occurredAt = occurredAt,
        message = message,
    )

    private fun event(
        scheduleId: ExternalScheduleId,
        runId: ExternalScheduleSyncRunId,
        sequence: Int,
        type: ExternalScheduleSyncJournalEventType,
        occurredAt: Instant,
        externalKey: String? = null,
        action: ExternalScheduleSyncAction? = null,
        eventId: String? = null,
        message: String? = null,
    ) = ExternalScheduleSyncJournalEvent(
        journalId = ExternalScheduleSyncJournalId(
            "11.32:${runId.value}:$sequence:${type.name}:${externalKey ?: "-"}",
        ),
        scheduleId = scheduleId,
        runId = runId,
        type = type,
        occurredAt = occurredAt,
        externalKey = externalKey,
        action = action,
        eventId = eventId,
        message = message,
    )
}
