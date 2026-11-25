package me.moiclathy.dism.external.schedule.sync.e2e

import kotlin.time.Instant
import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationRecord
import me.moiclathy.dism.external.schedule.sync.reconciliation.ExternalScheduleSyncReconciliationAction
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunId
import me.moiclathy.dism.external.schedule.sync.transaction.ExternalScheduleSyncRunRecord

/**
 * Previous-record resolution is kept outside reconciliation because 11.20
 * intentionally only decides what should happen.
 */
fun interface ExternalScheduleSyncPreviousRecordResolver {
    fun resolve(
        externalKey: String,
        currentRecord: ExternalSchedulePersistenceRecord?,
        reconciliationRecord: ExternalScheduleSyncReconciliationRecord,
        previousRecords: List<ExternalSchedulePersistenceRecord>,
    ): ExternalSchedulePersistenceRecord?
}

/**
 * Boundary used for COMPLETE_SYNC. It must only repair durable Sync state;
 * it must not create or mutate an Event.
 */
fun interface ExternalScheduleSyncCompleteHandler {
    fun complete(
        reconciliationRecord: ExternalScheduleSyncReconciliationRecord,
        now: Instant,
    ): String?
}

/**
 * Optional observation boundary. This makes 11.31 useful for audit/UI/event
 * streams without coupling the E2E runner to a concrete observation system.
 */
fun interface ExternalScheduleSyncE2EObservationSink {
    fun observe(trace: ExternalScheduleSyncE2EActionTrace)

    companion object {
        val NONE = ExternalScheduleSyncE2EObservationSink { }
    }
}

data class ExternalScheduleSyncE2EInput(
    val scheduleId: ExternalScheduleId,
    val runId: ExternalScheduleSyncRunId,
    val seed: ExternalScheduleSyncRunRecord,
    val projectedAt: Instant,
    val previousRecords: List<ExternalSchedulePersistenceRecord> = emptyList(),
    val previousRecordResolver: ExternalScheduleSyncPreviousRecordResolver =
        ExternalScheduleSyncPreviousRecordResolver { externalKey, _, _, records ->
            records.firstOrNull { it.externalKey == externalKey }
        },
    val completeSyncHandler: ExternalScheduleSyncCompleteHandler,
    val observationSink: ExternalScheduleSyncE2EObservationSink =
        ExternalScheduleSyncE2EObservationSink.NONE,
) {
    init {
        require(seed.scheduleId == scheduleId) {
            "seed scheduleId must match input scheduleId"
        }
        require(seed.runId == runId) {
            "seed runId must match input runId"
        }
    }
}
