package me.moiclathy.dism.external.schedule.sync.reconciliation

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncExecutionStatus
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord

/**
 * 确定性的 Sync / Event Reconciliation。
 */
class DeterministicExternalScheduleSyncReconciliationService(
    private val eventLookup:
        ExternalScheduleSyncEventLookup,
) : ExternalScheduleSyncReconciliationService {

    override fun reconcile(
        scheduleId:
            ExternalScheduleId,

        currentRecords:
            List<ExternalSchedulePersistenceRecord>,

        syncRecords:
            List<ExternalScheduleSyncPersistenceRecord>,
    ):
        ExternalScheduleSyncReconciliationResult {

        val currentByKey =
            currentRecords.associateBy {
                it.externalKey
            }

        val syncByKey =
            syncRecords
                .groupBy {
                    it.externalKey
                }
                .mapValues { (_, records) ->
                    records.maxByOrNull {
                        it.executedAt
                    }
                }

        val keys =
            (
                currentByKey.keys +
                    syncByKey.keys
                )
                .sorted()

        val results =
            mutableListOf<
                ExternalScheduleSyncReconciliationRecord
            >()

        val warnings =
            mutableListOf<String>()

        for (key in keys) {

            val current =
                currentByKey[key]

            val sync =
                syncByKey[key]

            val result =
                reconcileOne(
                    scheduleId =
                        scheduleId,

                    externalKey =
                        key,

                    current =
                        current,

                    sync =
                        sync,
                )

            results +=
                result
        }

        if (
            currentRecords.isEmpty() &&
            syncRecords.isNotEmpty()
        ) {

            warnings +=
                "Sync history exists without current external records"
        }

        return ExternalScheduleSyncReconciliationResult.success(
            records =
                results,

            warnings =
                warnings,
        )
    }

    private fun reconcileOne(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,

        current:
            ExternalSchedulePersistenceRecord?,

        sync:
            ExternalScheduleSyncPersistenceRecord?,
    ):
        ExternalScheduleSyncReconciliationRecord {

        if (
            sync == null
        ) {

            return reconcileWithoutSync(
                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                current =
                    current,
            )
        }

        val eventId =
            current?.eventId
                ?: sync.eventId

        val eventExists =
            !eventId.isNullOrBlank() &&
                eventLookup.exists(
                    eventId
                )

        if (
            sync.status ==
                ExternalScheduleSyncExecutionStatus
                    .CREATED ||
            sync.status ==
                ExternalScheduleSyncExecutionStatus
                    .UPDATED ||
            sync.status ==
                ExternalScheduleSyncExecutionStatus
                    .REMOVED
        ) {

            return reconcileSuccessfulSync(
                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                current =
                    current,

                sync =
                    sync,

                eventExists =
                    eventExists,
            )
        }

        if (
            sync.status ==
                ExternalScheduleSyncExecutionStatus
                    .FAILED
        ) {

            return reconcileFailedSync(
                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                current =
                    current,

                sync =
                    sync,

                eventExists =
                    eventExists,
            )
        }

        if (
            sync.status ==
                ExternalScheduleSyncExecutionStatus
                    .SKIPPED
        ) {

            return ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .CONSISTENT,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .NO_OP,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .SYNC_ALREADY_COMPLETED,

                syncRecord =
                    sync,

                currentRecord =
                    current,

                eventExists =
                    eventExists,
            )
        }

        return ExternalScheduleSyncReconciliationRecord(

            scheduleId =
                scheduleId,

            externalKey =
                externalKey,

            status =
                ExternalScheduleSyncReconciliationStatus
                    .UNKNOWN,

            action =
                ExternalScheduleSyncReconciliationAction
                    .NO_OP,

            reason =
                ExternalScheduleSyncReconciliationReason
                    .NO_SYNC_RECORD,

            syncRecord =
                sync,

            currentRecord =
                current,

            eventExists =
                eventExists,

            message =
                "Unknown sync status",
        )
    }

    private fun reconcileSuccessfulSync(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,

        current:
            ExternalSchedulePersistenceRecord?,

        sync:
            ExternalScheduleSyncPersistenceRecord,

        eventExists:
            Boolean,
    ):
        ExternalScheduleSyncReconciliationRecord {

        if (
            sync.action ==
                ExternalScheduleSyncAction
                    .HANDLE_REMOVED_EVENT
        ) {

            return if (
                eventExists
            ) {

                ExternalScheduleSyncReconciliationRecord(

                    scheduleId =
                        scheduleId,

                    externalKey =
                        externalKey,

                    status =
                        ExternalScheduleSyncReconciliationStatus
                            .CONSISTENT,

                    action =
                        ExternalScheduleSyncReconciliationAction
                            .NO_OP,

                    reason =
                        ExternalScheduleSyncReconciliationReason
                            .SYNC_ALREADY_COMPLETED,

                    syncRecord =
                        sync,

                    currentRecord =
                        current,

                    eventExists =
                        true,
                )

            } else {

                ExternalScheduleSyncReconciliationRecord(

                    scheduleId =
                        scheduleId,

                    externalKey =
                        externalKey,

                    status =
                        ExternalScheduleSyncReconciliationStatus
                            .CONSISTENT,

                    action =
                        ExternalScheduleSyncReconciliationAction
                            .NO_OP,

                    reason =
                        ExternalScheduleSyncReconciliationReason
                            .SYNC_ALREADY_COMPLETED,

                    syncRecord =
                        sync,

                    currentRecord =
                        current,

                    eventExists =
                        false,

                    message =
                        "Removal already completed",
                )
            }
        }

        if (
            eventExists
        ) {

            return ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .CONSISTENT,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .NO_OP,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .SYNC_ALREADY_COMPLETED,

                syncRecord =
                    sync,

                currentRecord =
                    current,

                eventExists =
                    true,
            )
        }

        return ExternalScheduleSyncReconciliationRecord(

            scheduleId =
                scheduleId,

            externalKey =
                externalKey,

            status =
                ExternalScheduleSyncReconciliationStatus
                    .MISSING_EVENT,

            action =
                when (
                    sync.action
                ) {

                    ExternalScheduleSyncAction
                        .CREATE_EVENT ->
                        ExternalScheduleSyncReconciliationAction
                            .RETRY_CREATE

                    ExternalScheduleSyncAction
                        .UPDATE_EVENT ->
                        ExternalScheduleSyncReconciliationAction
                            .RETRY_UPDATE

                    else ->
                        ExternalScheduleSyncReconciliationAction
                            .RECREATE_EVENT_MAPPING
                },

            reason =
                ExternalScheduleSyncReconciliationReason
                    .MISSING_EVENT_MAPPING,

            syncRecord =
                sync,

            currentRecord =
                current,

            eventExists =
                false,

            message =
                "Sync completed but mapped Event no longer exists",
        )
    }

    private fun reconcileFailedSync(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,

        current:
            ExternalSchedulePersistenceRecord?,

        sync:
            ExternalScheduleSyncPersistenceRecord,

        eventExists:
            Boolean,
    ):
        ExternalScheduleSyncReconciliationRecord {

        if (
            eventExists
        ) {

            return ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .FAILED_BUT_EVENT_EXISTS,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .COMPLETE_SYNC,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .SYNC_FAILED_EVENT_EXISTS,

                syncRecord =
                    sync,

                currentRecord =
                    current,

                eventExists =
                    true,

                message =
                    "Event exists despite failed Sync status",
            )
        }

        val action =
            when (
                sync.action
            ) {

                ExternalScheduleSyncAction
                    .CREATE_EVENT ->
                    ExternalScheduleSyncReconciliationAction
                        .RETRY_CREATE

                ExternalScheduleSyncAction
                    .UPDATE_EVENT ->
                    if (
                        current?.eventId.isNullOrBlank()
                    ) {

                        ExternalScheduleSyncReconciliationAction
                            .RECREATE_EVENT_MAPPING

                    } else {

                        ExternalScheduleSyncReconciliationAction
                            .RETRY_UPDATE
                    }

                ExternalScheduleSyncAction
                    .HANDLE_REMOVED_EVENT ->
                    ExternalScheduleSyncReconciliationAction
                        .RETRY_REMOVAL

                ExternalScheduleSyncAction
                    .RECREATE_EVENT_MAPPING ->
                    ExternalScheduleSyncReconciliationAction
                        .RECREATE_EVENT_MAPPING

                ExternalScheduleSyncAction
                    .NO_OP ->
                    ExternalScheduleSyncReconciliationAction
                        .NO_OP
            }

        return ExternalScheduleSyncReconciliationRecord(

            scheduleId =
                scheduleId,

            externalKey =
                externalKey,

            status =
                ExternalScheduleSyncReconciliationStatus
                    .FAILED_AND_EVENT_MISSING,

            action =
                action,

            reason =
                ExternalScheduleSyncReconciliationReason
                    .SYNC_FAILED_EVENT_MISSING,

            syncRecord =
                sync,

            currentRecord =
                current,

            eventExists =
                false,

            message =
                "Retry required because Event is missing",
        )
    }

    private fun reconcileWithoutSync(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,

        current:
            ExternalSchedulePersistenceRecord?,
    ):
        ExternalScheduleSyncReconciliationRecord {

        if (
            current == null
        ) {

            return ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .UNKNOWN,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .NO_OP,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .NO_SYNC_RECORD,
            )
        }

        if (
            !current.active
        ) {

            return ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .CONSISTENT,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .NO_OP,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .INACTIVE_EXTERNAL_RECORD,

                currentRecord =
                    current,

                eventExists =
                    false,
            )
        }

        val eventId =
            current.eventId

        if (
            eventId.isNullOrBlank()
        ) {

            return ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .MISSING_EVENT,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .RECREATE_EVENT_MAPPING,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .MISSING_EVENT_MAPPING,

                currentRecord =
                    current,

                eventExists =
                    false,
            )
        }

        val eventExists =
            eventLookup.exists(
                eventId
            )

        return if (
            eventExists
        ) {

            ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .CONSISTENT,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .NO_OP,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .NO_SYNC_RECORD,

                currentRecord =
                    current,

                eventExists =
                    true,
            )

        } else {

            ExternalScheduleSyncReconciliationRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                status =
                    ExternalScheduleSyncReconciliationStatus
                        .MISSING_EVENT,

                action =
                    ExternalScheduleSyncReconciliationAction
                        .RECREATE_EVENT_MAPPING,

                reason =
                    ExternalScheduleSyncReconciliationReason
                        .UNKNOWN_EVENT_MAPPING,

                currentRecord =
                    current,

                eventExists =
                    false,
            )
        }
    }
}
