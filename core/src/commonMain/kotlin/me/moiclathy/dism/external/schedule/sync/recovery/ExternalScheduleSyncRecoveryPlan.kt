package me.moiclathy.dism.external.schedule.sync.recovery

import me.moiclathy.dism.external.schedule.ExternalScheduleId

/**
 * 一次 Recovery 的完整计划。
 */
data class ExternalScheduleSyncRecoveryPlan(

    val scheduleId:
        ExternalScheduleId,

    val records:
        List<ExternalScheduleSyncRecoveryRecord>,
) {

    val retryCreates:
        List<ExternalScheduleSyncRecoveryRecord>
        get() =
            records.filter {
                it.action ==
                    ExternalScheduleSyncRecoveryAction
                        .RETRY_CREATE
            }

    val retryUpdates:
        List<ExternalScheduleSyncRecoveryRecord>
        get() =
            records.filter {
                it.action ==
                    ExternalScheduleSyncRecoveryAction
                        .RETRY_UPDATE
            }

    val retryRemovals:
        List<ExternalScheduleSyncRecoveryRecord>
        get() =
            records.filter {
                it.action ==
                    ExternalScheduleSyncRecoveryAction
                        .RETRY_REMOVAL
            }

    val recreateMappings:
        List<ExternalScheduleSyncRecoveryRecord>
        get() =
            records.filter {
                it.action ==
                    ExternalScheduleSyncRecoveryAction
                        .RECREATE_EVENT_MAPPING
            }

    val reconciliations:
        List<ExternalScheduleSyncRecoveryRecord>
        get() =
            records.filter {
                it.action ==
                    ExternalScheduleSyncRecoveryAction
                        .RECONCILE
            }

    val skipped:
        List<ExternalScheduleSyncRecoveryRecord>
        get() =
            records.filter {
                it.action ==
                    ExternalScheduleSyncRecoveryAction
                        .SKIP
            }

    val recoverableCount: Int
        get() =
            records.count {
                it.recoverable
            }

    val skippedCount: Int
        get() =
            skipped.size
}
