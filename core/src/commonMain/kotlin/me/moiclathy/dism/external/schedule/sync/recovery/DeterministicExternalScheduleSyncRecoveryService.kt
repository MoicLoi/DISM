package me.moiclathy.dism.external.schedule.sync.recovery

import me.moiclathy.dism.external.schedule.ExternalScheduleId
import me.moiclathy.dism.external.schedule.persistence.ExternalSchedulePersistenceRecord
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncAction
import me.moiclathy.dism.external.schedule.sync.ExternalScheduleSyncExecutionStatus
import me.moiclathy.dism.external.schedule.sync.persistence.ExternalScheduleSyncPersistenceRecord

/**
 * 确定性的 Sync Recovery Service。
 */
class DeterministicExternalScheduleSyncRecoveryService(
    private val repository:
        ExternalScheduleSyncRecoveryRepository,
) : ExternalScheduleSyncRecoveryService {

    override fun recover(
        scheduleId:
            ExternalScheduleId,

        currentRecords:
            List<ExternalSchedulePersistenceRecord>,

        policy:
            ExternalScheduleSyncRecoveryPolicy,
    ): ExternalScheduleSyncRecoveryResult {

        val latestHistory =
            repository.findLatestRun(
                scheduleId
            )

        val historyByKey =
            latestHistory.associateBy {
                it.externalKey
            }

        val currentByKey =
            currentRecords.associateBy {
                it.externalKey
            }

        val keys =
            (
                historyByKey.keys +
                    currentByKey.keys
                )
                .sorted()

        val records =
            mutableListOf<
                ExternalScheduleSyncRecoveryRecord
            >()

        val warnings =
            mutableListOf<String>()

        for (key in keys) {

            val history =
                historyByKey[key]

            val current =
                currentByKey[key]

            val recovery =
                resolve(
                    scheduleId =
                        scheduleId,

                    externalKey =
                        key,

                    history =
                        history,

                    current =
                        current,

                    policy =
                        policy,
                )

            records += recovery
        }

        if (
            latestHistory.isEmpty() &&
            currentRecords.isNotEmpty()
        ) {

            warnings +=
                "No previous sync history found; " +
                    "current records require initial reconciliation"
        }

        val plan =
            ExternalScheduleSyncRecoveryPlan(

                scheduleId =
                    scheduleId,

                records =
                    records,
            )

        return ExternalScheduleSyncRecoveryResult.success(
            plan =
                plan,

            warnings =
                warnings,
        )
    }

    private fun resolve(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,

        history:
            ExternalScheduleSyncPersistenceRecord?,

        current:
            ExternalSchedulePersistenceRecord?,

        policy:
            ExternalScheduleSyncRecoveryPolicy,
    ): ExternalScheduleSyncRecoveryRecord {

        if (
            history == null &&
            current == null
        ) {

            return ExternalScheduleSyncRecoveryRecord(

                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                action =
                    ExternalScheduleSyncRecoveryAction
                        .SKIP,

                reason =
                    ExternalScheduleSyncRecoveryReason
                        .INVALID_SYNC_RECORD,

                message =
                    "No current record or sync history",
            )
        }

        if (
            history != null &&
            history.status ==
                ExternalScheduleSyncExecutionStatus
                    .FAILED
        ) {

            return resolveFailed(
                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                history =
                    history,

                current =
                    current,

                policy =
                    policy,
            )
        }

        if (
            current != null &&
            current.active &&
            current.eventId.isNullOrBlank()
        ) {

            return if (
                policy.recreateMissingMapping
            ) {

                ExternalScheduleSyncRecoveryRecord(

                    scheduleId =
                        scheduleId,

                    externalKey =
                        externalKey,

                    action =
                        ExternalScheduleSyncRecoveryAction
                            .RECREATE_EVENT_MAPPING,

                    reason =
                        ExternalScheduleSyncRecoveryReason
                            .MISSING_EVENT_MAPPING,

                    syncRecord =
                        history,

                    currentRecord =
                        current,

                    message =
                        "Active external record has no Event mapping",
                )

            } else if (
                policy.reconcileMissingMapping
            ) {

                ExternalScheduleSyncRecoveryRecord(

                    scheduleId =
                        scheduleId,

                    externalKey =
                        externalKey,

                    action =
                        ExternalScheduleSyncRecoveryAction
                            .RECONCILE,

                    reason =
                        ExternalScheduleSyncRecoveryReason
                            .MISSING_EVENT_MAPPING,

                    syncRecord =
                        history,

                    currentRecord =
                        current,

                    message =
                        "Active external record requires reconciliation",
                )

            } else {

                skipped(
                    scheduleId =
                        scheduleId,

                    externalKey =
                        externalKey,

                    history =
                        history,

                    current =
                        current,

                    reason =
                        ExternalScheduleSyncRecoveryReason
                            .MISSING_EVENT_MAPPING,
                )
            }
        }

        if (
            current != null &&
            !current.active
        ) {

            return skipped(
                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                history =
                    history,

                current =
                    current,

                reason =
                    ExternalScheduleSyncRecoveryReason
                        .INACTIVE_EXTERNAL_RECORD,
            )
        }

        if (
            history != null &&
            history.successful &&
            policy.skipSuccessful
        ) {

            return skipped(
                scheduleId =
                    scheduleId,

                externalKey =
                    externalKey,

                history =
                    history,

                current =
                    current,

                reason =
                    ExternalScheduleSyncRecoveryReason
                        .SUCCESSFUL_PREVIOUS_SYNC,
            )
        }

        return ExternalScheduleSyncRecoveryRecord(

            scheduleId =
                scheduleId,

            externalKey =
                externalKey,

            action =
                ExternalScheduleSyncRecoveryAction
                    .RECONCILE,

            reason =
                ExternalScheduleSyncRecoveryReason
                    .ALREADY_RECONCILED,

            syncRecord =
                history,

            currentRecord =
                current,

            message =
                "State requires reconciliation",
        )
    }

    private fun resolveFailed(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,

        history:
            ExternalScheduleSyncPersistenceRecord,

        current:
            ExternalSchedulePersistenceRecord?,

        policy:
            ExternalScheduleSyncRecoveryPolicy,
    ): ExternalScheduleSyncRecoveryRecord {

        return when (
            history.action
        ) {

            ExternalScheduleSyncAction
                .CREATE_EVENT -> {

                if (
                    policy.retryFailedCreate &&
                    current != null
                ) {

                    ExternalScheduleSyncRecoveryRecord(

                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        action =
                            ExternalScheduleSyncRecoveryAction
                                .RETRY_CREATE,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .FAILED_CREATE,

                        syncRecord =
                            history,

                        currentRecord =
                            current,

                        message =
                            "Retry failed Event creation",
                    )

                } else {

                    skipped(
                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        history =
                            history,

                        current =
                            current,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .FAILED_CREATE,
                    )
                }
            }

            ExternalScheduleSyncAction
                .UPDATE_EVENT -> {

                if (
                    policy.retryFailedUpdate &&
                    current != null &&
                    !current.eventId.isNullOrBlank()
                ) {

                    ExternalScheduleSyncRecoveryRecord(

                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        action =
                            ExternalScheduleSyncRecoveryAction
                                .RETRY_UPDATE,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .FAILED_UPDATE,

                        syncRecord =
                            history,

                        currentRecord =
                            current,

                        message =
                            "Retry failed Event update",
                    )

                } else if (
                    policy.reconcileMissingMapping &&
                    current != null
                ) {

                    ExternalScheduleSyncRecoveryRecord(

                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        action =
                            ExternalScheduleSyncRecoveryAction
                                .RECONCILE,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .MISSING_EVENT_MAPPING,

                        syncRecord =
                            history,

                        currentRecord =
                            current,

                        message =
                            "Update failed or Event mapping is unavailable",
                    )

                } else {

                    skipped(
                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        history =
                            history,

                        current =
                            current,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .FAILED_UPDATE,
                    )
                }
            }

            ExternalScheduleSyncAction
                .RECREATE_EVENT_MAPPING -> {

                if (
                    policy.reconcileMissingMapping &&
                    current != null
                ) {

                    ExternalScheduleSyncRecoveryRecord(

                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        action =
                            ExternalScheduleSyncRecoveryAction
                                .RECONCILE,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .MISSING_EVENT_MAPPING,

                        syncRecord =
                            history,

                        currentRecord =
                            current,

                        message =
                            "Recreate missing Event mapping",
                    )

                } else {

                    skipped(
                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        history =
                            history,

                        current =
                            current,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .MISSING_EVENT_MAPPING,
                    )
                }
            }

            ExternalScheduleSyncAction
                .HANDLE_REMOVED_EVENT -> {

                if (
                    policy.retryFailedRemoval
                ) {

                    ExternalScheduleSyncRecoveryRecord(

                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        action =
                            ExternalScheduleSyncRecoveryAction
                                .RETRY_REMOVAL,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .FAILED_REMOVAL,

                        syncRecord =
                            history,

                        currentRecord =
                            current,

                        message =
                            "Retry failed removed Event handling",
                    )

                } else {

                    skipped(
                        scheduleId =
                            scheduleId,

                        externalKey =
                            externalKey,

                        history =
                            history,

                        current =
                            current,

                        reason =
                            ExternalScheduleSyncRecoveryReason
                                .FAILED_REMOVAL,
                    )
                }
            }

            ExternalScheduleSyncAction
                .NO_OP -> {

                skipped(
                    scheduleId =
                        scheduleId,

                    externalKey =
                        externalKey,

                    history =
                        history,

                    current =
                        current,

                    reason =
                        ExternalScheduleSyncRecoveryReason
                            .SUCCESSFUL_PREVIOUS_SYNC,
                )
            }
        }
    }

    private fun skipped(
        scheduleId:
            ExternalScheduleId,

        externalKey:
            String,

        history:
            ExternalScheduleSyncPersistenceRecord?,

        current:
            ExternalSchedulePersistenceRecord?,

        reason:
            ExternalScheduleSyncRecoveryReason,
    ): ExternalScheduleSyncRecoveryRecord {

        return ExternalScheduleSyncRecoveryRecord(

            scheduleId =
                scheduleId,

            externalKey =
                externalKey,

            action =
                ExternalScheduleSyncRecoveryAction
                    .SKIP,

            reason =
                reason,

            syncRecord =
                history,

            currentRecord =
                current,
        )
    }
}
